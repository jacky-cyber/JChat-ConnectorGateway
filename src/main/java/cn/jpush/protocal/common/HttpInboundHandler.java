package cn.jpush.protocal.common;

import java.io.File;
import java.io.IOException;

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
	private HttpRequest request;
	private boolean readingChunks;
	private final StringBuilder responseContent = new StringBuilder();
	private static final HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE);
	private HttpPostRequestDecoder decoder;
	static {
		DiskFileUpload.deleteOnExitTemporaryFile = true; 
		DiskFileUpload.baseDirectory = null; 
		DiskAttribute.deleteOnExitTemporaryFile = true; 
		DiskAttribute.baseDirectory = null;
	}
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
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
			try {
				decoder = new HttpPostRequestDecoder(factory, request);
			} catch (ErrorDataDecoderException e1) {
				e1.printStackTrace();
				ctx.channel().close();
				return;
			} catch (IncompatibleDataDecoderException e1) {
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
					e1.printStackTrace();
					ctx.channel().close();
					return;
				}
				readHttpDataChunkByChunk();
				if (chunk instanceof LastHttpContent) {
					readingChunks = false;
				}
			}
		} 
		String res = "Upload Success";
      FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(res.getBytes("UTF-8")));
      response.headers().set("content-type", "text/plain");
      response.headers().set("content-length", response.content().readableBytes());
      if (HttpHeaders.isKeepAlive(request)) {
         response.headers().set("connection", Values.KEEP_ALIVE);
        }
      ctx.write(response);
      ctx.flush();
	}
	
	private void readHttpDataChunkByChunk() {
		try {
			while (decoder.hasNext()) {
				InterfaceHttpData data = decoder.next();
				if (data != null) {
					try {
						if (data.getHttpDataType() == HttpDataType.Attribute) {
							Attribute attribute = (Attribute) data;
							String value;
							try {
								value = attribute.getValue();
								System.out.println("value: "+value);
							} catch (IOException e1) {
								e1.printStackTrace();
								return;
							}	
						} else if(data.getHttpDataType() == HttpDataType.FileUpload){
							FileUpload fileUpload = (FileUpload) data;
							if (fileUpload.isCompleted()) {
								try {
									File file = fileUpload.getFile();
									System.out.println("path:"+file.getAbsolutePath());
									System.out.println("size:"+file.length());
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
					} finally {
						data.release();
					}
				}
			}
		} catch (EndOfDataDecoderException e1) {
				e1.printStackTrace();
		}
	}

}
