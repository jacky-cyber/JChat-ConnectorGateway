package cn.jpush.protocal.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import ch.qos.logback.classic.Logger;
import cn.jpush.protocal.utils.StringUtils;
import cn.jpush.protocal.utils.SystemConfig;
import cn.jpush.webim.server.WebIMFileServer;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.HttpHeaders.Values;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.EndOfDataDecoderException;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.ErrorDataDecoderException;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.IncompatibleDataDecoderException;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.handler.codec.http.multipart.MixedAttribute;

public class HttpInboundHandler extends SimpleChannelInboundHandler<HttpObject> {
	private static Logger log = (Logger) LoggerFactory.getLogger(HttpInboundHandler.class);
	private static final String FILE_STORE_PATH = SystemConfig.getProperty("im.file.store.path");
	private static final String APP_KEY = SystemConfig.getProperty("appKey");
	private static final String MASTER_SECRECT = SystemConfig.getProperty("masterSecrect");
	private HttpRequest request;
	private boolean readingChunks;
	private static final HttpDataFactory factory = new DefaultHttpDataFactory(true);
	private String filePath;
	private String fileId;
	private HttpPostRequestDecoder decoder;
	static {
		DiskFileUpload.deleteOnExitTemporaryFile = true; 
		DiskFileUpload.baseDirectory = null; 
		DiskAttribute.deleteOnExitTemporaryFile = true; 
		DiskAttribute.baseDirectory = null;
	}
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws UnsupportedEncodingException {
		ctx.flush();
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
	    ctx.close();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg)
			throws Exception {
		if (msg instanceof HttpRequest) {
			HttpRequest request = this.request = (HttpRequest) msg;
			String uri = request.getUri();
		
			log.info("signature request uri: "+uri);
			uri = uri.split("[?]")[0];
			if("/signature".equals(uri)||"/signature/".equals(uri)){
				this.responseSignatureInfo(ctx);
				return;
			}
			try {
				decoder = new HttpPostRequestDecoder(factory, request);
			} catch (ErrorDataDecoderException e1) {
				log.warn(e1.getMessage());
				return;
			} catch (IncompatibleDataDecoderException e1) {
				log.warn(e1.getMessage());
				return;
			}
			readingChunks = HttpHeaders.isTransferEncodingChunked(request);
			if (readingChunks) {
				readingChunks = true;
			}
		}
			
		if (decoder != null) {
			if (msg instanceof HttpContent) {
				HttpContent chunk = (HttpContent) msg;
				try {
					decoder.offer(chunk);
				} catch (ErrorDataDecoderException e1) {
					log.warn(e1.getMessage());
					return;
				}
				filePath = readHttpDataChunkByChunk(ctx);
				if (chunk instanceof LastHttpContent) {
					readingChunks = false;
					log.info("lastcontent");
					if(filePath!=null&&!"".equals(filePath)){
						this.responseUploadSuccess(ctx, filePath);
					} else {
						this.responseUploadError(ctx);
					}
				}
			}
		} 
		
	}

	private String readHttpDataChunkByChunk(ChannelHandlerContext ctx) throws IOException {
		String filePath = "";
		while (decoder.hasNext()) {
			InterfaceHttpData data = decoder.next();
			if (data != null) {
				try {
					if (data.getHttpDataType() == HttpDataType.Attribute) {
						Attribute attribute = (Attribute) data;
						fileId = attribute.getValue();
						log.info("fileId: "+fileId);
					} else if (data.getHttpDataType() == HttpDataType.FileUpload) {
						FileUpload fileUpload = (FileUpload) data;
						if (fileUpload.isCompleted()) {
							try {
								File file = fileUpload.getFile();
								String[] str = file.getName().split("\\.");
								String type = str[str.length-1];
								filePath = FILE_STORE_PATH + fileId/* + "." + type*/;
								log.info("filepath: "+filePath);
								File desFile = new File(filePath);
								int byteread = 0;
								InputStream in = null;
								OutputStream out = null;
								try {
									in = new FileInputStream(file);
									out = new FileOutputStream(desFile);
									byte[] buffer = new byte[1024];
									while ((byteread = in.read(buffer)) != -1) {
										out.write(buffer, 0, byteread);
									}
								} catch (FileNotFoundException e) {
									log.warn(e.getMessage());
									e.printStackTrace();
								} catch (IOException e) {
									log.warn(e.getMessage());
									e.printStackTrace();
								} finally {
									try {
										if (out != null)
											out.close();
										if (in != null)
											in.close();
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
								log.info("upload file: "+file.getName());
							} catch (IOException e) {
								log.warn(e.getMessage());
								e.printStackTrace();
								return null;
							}
						}
					}
				} finally {
					data.release();
				}
			}
		}
		return filePath;
	}

	public void responseUploadError(ChannelHandlerContext ctx) throws UnsupportedEncodingException{
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("result", false);
		Gson gson = new Gson();
		String result = gson.toJson(data);
		log.info("json: "+result);
	   FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.SERVICE_UNAVAILABLE, Unpooled.wrappedBuffer(result.getBytes("UTF-8")));
	   response.headers().set("content-type", "application/json;charset=utf-8");
	   response.headers().set("content-length", response.content().readableBytes());
	   ctx.write(response);
	   ctx.flush();
	}
	
	public void responseUploadSuccess(ChannelHandlerContext ctx, String filePath) throws UnsupportedEncodingException{
	   Map<String, Object> data = new HashMap<String, Object>();
	   data.put("result", true);
	   data.put("data", filePath);
	   Gson gson = new Gson();
	   String result = gson.toJson(data);
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(result.getBytes("UTF-8")));
	   response.headers().set("content-type", "application/json;charset=utf-8");
	   response.headers().set("content-length", response.content().readableBytes());
	   ctx.write(response);
	   ctx.flush();
	}
	
	private void responseSignatureInfo(ChannelHandlerContext ctx) throws UnsupportedEncodingException{
		String timeStamp = this.getTimestamp();
		String randomStr = this.getRandomStr();
		String signature = this.getSignature(APP_KEY, timeStamp, randomStr, MASTER_SECRECT);
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("timeStamp", timeStamp);
		data.put("randomStr", randomStr);
		data.put("signature", signature);
		Gson gson = new Gson();
		String result = gson.toJson(data);
		result = "jsonpCallback("+result+")";
		log.info("response signature data: "+result);
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(result.getBytes("UTF-8")));
		response.headers().set("content-type", "application/json;charset=utf-8");
		response.headers().set("content-length", response.content().readableBytes());
		ctx.write(response);
		ctx.flush();
	}
	
	private String getSignature(String appKey, String timestamp, String randomStr, String masterSecrect){
    	 String str = "appKey=" + appKey +
                 "&timestamp=" + timestamp +
                 "&randomStr=" + randomStr +
                 "&masterSecrect=" + masterSecrect;
    	 return StringUtils.toMD5(str);
    }

	private String getRandomStr() {
        return UUID.randomUUID().toString();
    }

    private String getTimestamp() {
        return Long.toString(System.currentTimeMillis() / 1000);
    }
}
