/***** 缩略图处理  *****/
	var allowExt = ['jpg', 'gif', 'bmp', 'png', 'jpeg']; 
	var preivew = function(file, container){ 
	    try{ 
	        var pic =  new Picture(file, document.getElementById(container)); 
	    }catch(e){ 
	        alert(e); 
	    } 
	};
	//缩略图类定义 
	var Picture  = function(file, container){ 
	    var height = 0, 
	    widht  = 0, 
	    ext    = '', 
	    size   = 0, 
	    name   = '', 
	    path   =  ''; 
	    var self   = this; 
	    if(file){ 
	        name = file.value; 
	        if(window.navigator.userAgent.indexOf("MSIE")>=1){ 
	            file.select(); 
	            path = document.selection.createRange().text; 
	        }else if(window.navigator.userAgent.indexOf("Firefox")>=1){  
	            if(file.files){ 
	                //path =  file.files.item(0).getAsDataURL(); 
	                var path = window.URL.createObjectURL(file.files[0]);
	            }else{ 
	                path = file.value; 
	            } 
	        } 
	    }else{ 
	        throw '无效的文件'; 
	    } 
	   ext = name.substr(name.lastIndexOf("."), name.length); 
	   if(container.tagName.toLowerCase() != 'img'){ 
	        throw '不是一个有效的图片容器'; 
	        container.visibility = 'hidden'; 
	    } 
	   container.src = path; 
	   container.alt = name; 
	   container.style.visibility = 'visible'; 
	   height = container.height; 
	   width  = container.width; 
	   size   = container.fileSize; 
	   this.get = function(name){ 
	       return self[name]; 
	    } 
	   this.isValid = function(){ 
	       if(allowExt.indexOf(self.ext) !== -1){ 
	           throw '不允许上传该文件类型'; 
	           return false; 
	       } 
	    } 
	}; 
	/*****************/
	//  缩略图预览
	/*$('#picfile').live('change', function(){
		preivew(this, 'preview-pic');
	});*/
	
//  新消息滚动提示  
function flash_title()
{
  //当窗口效果为最小化，或者没焦点状态下才闪动
  if(isMinStatus() || !window.focus){
	  newMsgCount();
  }
  else{
    document.title='JPush IM';//窗口没有消息的时候默认的title内容
    window.clearInterval();
  }
}

//消息提示
var flag=false;
function newMsgCount(){
  if(flag){
    flag=false;
    document.title='【新消息】';
  }else{
    flag=true;
    document.title='【　　　】';
  }
  window.setTimeout('flash_title(0)',380);
}

//判断窗口是否最小化
//在Opera中还不能显示
var isMin = false;
function isMinStatus() {
  //除了Internet Explorer浏览器，其他主流浏览器均支持Window outerHeight 和outerWidth 属性
  if(window.outerWidth != undefined && window.outerHeight != undefined){
    isMin = window.outerWidth <= 160 && window.outerHeight <= 27;
  }else{
    isMin = window.outerWidth <= 160 && window.outerHeight <= 27;
  }
  //除了Internet Explorer浏览器，其他主流浏览器均支持Window screenY 和screenX 属性
  if(window.screenY != undefined && window.screenX != undefined ){
    isMin = window.screenY < -30000 && window.screenX < -30000;//FF Chrome      
  }else{
    isMin = window.screenTop < -30000 && window.screenLeft < -30000;//IE
  }
  return isMin;
}

var recvMsgNotifacation = function(){
	
}

