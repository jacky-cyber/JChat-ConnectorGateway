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


// 获取http请求参数
function getUrlParam(name) {
	var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)");
	var r = window.location.search.substr(1).match(reg);
	if (r != null)
		return unescape(r[2]);
	return null;
} 

var curUserId = null;  //  当前用户id
var curChatUserId = null;  //  当前聊天对象id
var curChatGroupId = null;  // 当前聊天Group id
var curChatUserSessionId  = null;  // data session_id
var preChatUserId = null;  // 前一个聊天对象
var preChatGroupId = null; // 前一个群组对象
var msgCardDivId = "chat01";
var talkToDivId = "talkTo";
var isSingleOrGroup = "single";  //  区分是单聊还是群聊
var user_name = getUrlParam("user_name");
curUserId = user_name;


//  事件绑定
//var socket = io.connect("http://127.0.0.1:9092", {'reconnect':true,'upgrade':true});
var socket = io.connect("http://127.0.0.1:9092",{'transports':['websocket']});  //  websocket
//var socket = io.connect("http://127.0.0.1:9092");  //  polling
socket.on('connect', function(){
	var message = 'user:'+user_name+'login';
	socket.emit('loginevent', {userName: user_name, message: message});  // 连接成功后触发登陆，发送登陆消息
	socket.emit('getContracterList', {user_name: user_name});   // 获取联系人列表
	socket.emit('getGroupsList', {user_name: user_name});   // 获取群组列表
});

// 监听获取联系人
socket.on('getContracterList', function(data){
	createContactlistUL();  // 创建联系人列表UI
	var uielem = document.getElementById("contactlistUL");
	for (i = 0; i < data.length; i++) {
		var lielem = document.createElement("li");
		$(lielem).attr({
			'id' : data[i].user_name,
			'sessionId' : data[i].session_id,
			'class' : 'online',
			'className' : 'online',
			'chat' : 'chat',
			'displayName' : data[i].user_name,
			'online' : data[i].online
		});
		if(data[i].online){
			$(lielem).css({
				"background": "#B0C4DE"
			});
		} else {
			$(lielem).css({
				"background": "#708090"
			});
		}
		lielem.onclick = function() {
			chooseContactDivClick(this);
		};
		var imgelem = document.createElement("img");
		imgelem.setAttribute("src", "../../res/img/head/contact_normal.png");
		lielem.appendChild(imgelem);

		var spanelem = document.createElement("span");
		spanelem.innerHTML = data[i].user_name;
		lielem.appendChild(spanelem);
		uielem.appendChild(lielem);
	}
	var contactlist = document.getElementById('contractlist');
	var children = contactlist.children;
	if (children.length > 0) {
		contactlist.removeChild(children[0]);
	}
	contactlist.appendChild(uielem);
	//  默认选择与第一个联系人聊天
	if(data.length>0){
		setCurrentContact(data[0].user_name);
	}
});

//监听获取群组
socket.on('getGroupsList', function(data){
	createGroupslistUL();  // 创建群组列表UI
	var uielem = document.getElementById("grouplistUL");
	for (i = 0; i < data.length; i++) {
		var lielem = document.createElement("li");
		$(lielem).attr({
			'id' : data[i].group_id,
			'chat' : 'chat',
			'displayName' : data[i].group_name,
		});
		
		lielem.onclick = function() {
			chooseGroupDivClick(this);
		};
		var imgelem = document.createElement("img");
		imgelem.setAttribute("src", "../../res/img/head/group_normal.png");
		lielem.appendChild(imgelem);

		var spanelem = document.createElement("span");
		spanelem.innerHTML = data[i].group_name;
		lielem.appendChild(spanelem);
		uielem.appendChild(lielem);
	}
	var grouplist = document.getElementById('grouplist');
	var children = grouplist.children;
	if (children.length > 0) {
		grouplist.removeChild(children[0]);
	}
	grouplist.appendChild(uielem);
	//  默认选择与第一个群组
	curChatGroupId = data[0].group_id;
	//createGroupChatDiv(curChatGroupId);
	preChatGroupId = curChatGroupId;
});

//  监听用户聊天
socket.on('chatevent',function(data){
	appendMsgSendByOthers(data.userName, data.message, data.toUserName, data.msgType);
});

//  监听用户断开
socket.on('disconnect',function(){
	console.log('disconnect to the server.');
});
	
//  左边栏tab处理
$('#friendsTab').click(function(){
	$("#contractlist").css({
		display:"block"
	});
	$('#grouplist').css({
		display:"none"
	});
});

$('#groupsTab').click(function(){
	$('#grouplist').css({
		display:"block"
	});
	$("#contractlist").css({
		display:"none"
	});
});

//  回车键发送消息
function stopDefault(e) {  
    //如果提供了事件对象，则这是一个非IE浏览器   
    if(e && e.preventDefault) {  
    	e.preventDefault();  
    } else {  
    	window.event.returnValue = false;   
    }  
    return false;  
}  
document.onkeydown=function(event){
	var e = event || window.event || arguments.callee.caller.arguments[0];      
   if(e && e.keyCode==13){ // enter
	   stopDefault(e);
	   var content = document.getElementById('talkInputId').value;
	   if(content!=''){
		   appendMsgSendByMe(content);
		   if(isSingleOrGroup=='single'){
		   	socket.emit('chatevent', {userName: user_name, toUserName: curChatUserId, message: content, msgType:'single'});
		   } else if(isSingleOrGroup=='group'){
		   	socket.emit('chatevent', {userName: user_name, toUserName: curChatGroupId, message: content, msgType:'group'});
		    } 
	   } else {
		   alert('您还未输入.');
	   }
	   document.getElementById('talkInputId').value = '';
    }
}; 

// 发送消息
function sendText(){
	 var content = document.getElementById('talkInputId').value;
	 if(content==''){
		  alert('您还未输入.');
		  return;
	  }
 	 appendMsgSendByMe(content);
    document.getElementById('talkInputId').value = '';
    if(isSingleOrGroup=='single'){
    	socket.emit('chatevent', {userName: user_name, toUserName: curChatUserId, message: content, msgType:'single'});
    } else if(isSingleOrGroup=='group'){
    	socket.emit('chatevent', {userName: user_name, toUserName: curChatGroupId, message: content, msgType:'group'});
     } 
};
	 
//获取当前聊天记录的窗口div
var getContactChatDiv = function(chatUserId) {
	return document.getElementById(curUserId + "-" + chatUserId);
};

//获取当前群组聊天记录的窗口
var getGroupChatDiv = function(chatGroupId) {
	return document.getElementById(curUserId + "-" + chatGroupId);
};
	
// 创建一个联系人聊天窗口
var createContactChatDiv = function(chatUserId) {
	var msgContentDivId = curUserId + "-" + chatUserId;
	var newContent = document.createElement("div");
	newContent.setAttribute("id", msgContentDivId);
	newContent.setAttribute("class", "chat01_content");
	newContent.setAttribute("className", "chat01_content");
	newContent.setAttribute("style", "display:none");
	return newContent;
};

//  创建一个群组聊天窗口
var createGroupChatDiv = function(chatGroupId) {
	var msgContentDivId = curUserId + "-" + chatGroupId;
	var newContent = document.createElement("div");
	newContent.setAttribute("id", msgContentDivId);
	newContent.setAttribute("class", "chat01_content");
	newContent.setAttribute("className", "chat01_content");
	newContent.setAttribute("style", "display:none");
	return newContent;
};
	
// 创建联系人列表UI
var createContactlistUL = function() {
	var uielem = document.createElement("ul");
	$(uielem).attr({
		"id" : "contactlistUL",
		"class" : "chat03_content_ul"
	});
	var contactlist = document.getElementById("contractlist");
	contactlist.appendChild(uielem);
};

//创建群组列表UI
var createGroupslistUL = function() {
	var uielem = document.createElement("ul");
	$(uielem).attr({
		"id" : "grouplistUL",
		"class" : "chat03_content_ul"
	});
	var contactlist = document.getElementById("grouplist");
	contactlist.appendChild(uielem);
};
	
// 设置当前联系人界面
var setCurrentContact = function(defaultUserId) {
	showContactChatDiv(defaultUserId);
	if (curChatUserId != null) {
		hiddenContactChatDiv(curChatUserId);
	} else {
		$('#null-nouser').css({
			"display" : "none"
		});
	}
	curChatUserId = defaultUserId;
	preChatUserId = curChatUserId;
};
	
// 显示与联系人聊天的窗口
var showContactChatDiv = function(chatUserId) {
	var contentDiv = getContactChatDiv(chatUserId);
	if (contentDiv == null) {
		contentDiv = createContactChatDiv(chatUserId);
		document.getElementById(msgCardDivId).appendChild(contentDiv);
	}
	contentDiv.style.display = "block";
	var contactLi = document.getElementById(chatUserId);
	if (contactLi == null) {
		return;
	}
	contactLi.style.backgroundColor = "#B0E0E6";
	var dispalyTitle = "与 " + chatUserId + " 聊天中";
	document.getElementById(talkToDivId).children[0].innerHTML = dispalyTitle;
};

//  显示群组的聊天窗口
var showGroupChatDiv = function(chatGroupId) {
	var contentDiv = getGroupChatDiv(chatGroupId);
	if (contentDiv == null) {
		contentDiv = createGroupChatDiv(chatGroupId);
		document.getElementById(msgCardDivId).appendChild(contentDiv);
	}
	contentDiv.style.display = "block";
	var contactLi = document.getElementById(chatGroupId);
	if (contactLi == null) {
		return;
	}
	contactLi.style.backgroundColor = "#B0E0E6";
	var group_name = $('#'+chatGroupId).attr("displayName");
	var dispalyTitle = "正在 " + group_name + " 群里愉快的聊天中";
	document.getElementById(talkToDivId).children[0].innerHTML = dispalyTitle;
};
	
//对上一个联系人的聊天窗口做隐藏处理
var hiddenContactChatDiv = function(chatUserId) {
	var contactLi = document.getElementById(chatUserId);
	if (contactLi) {
		contactLi.style.backgroundColor = "";
	}
	var contentDiv = getContactChatDiv(chatUserId);
	if (contentDiv) {
		contentDiv.style.display = "none";
	}
};

//对上一个群组的聊天窗口做隐藏处理
var hiddenGroupChatDiv = function(chatGroupId) {
	var contactLi = document.getElementById(chatGroupId);
	if (contactLi) {
		contactLi.style.backgroundColor = "";
	}
	var contentDiv = getGroupChatDiv(chatGroupId);
	if (contentDiv) {
		contentDiv.style.display = "none";
	}
};

// 切换联系人聊天窗口div
var chooseContactDivClick = function(li) {
	var chatUserId = li.id;
	curChatUserSessionId = li.sessionId;
	if ((chatUserId != curChatUserId)) {
		if (curChatUserId == null) {
			createContactChatDiv(chatUserId);
		} else {
			showContactChatDiv(chatUserId);
			hiddenContactChatDiv(curChatUserId);
		}
	} else {
		showContactChatDiv(chatUserId);
	}
	if(curChatGroupId != null){
		hiddenGroupChatDiv(curChatGroupId);
	}
	curChatUserId = chatUserId;
	/*$('#null-nouser').css({
		"display" : "none"
	});*/
	// 对前一个聊天用户的背景作处理
	var preUser = $('#'+preChatUserId);
	if(preUser.attr('online')=='true'){
		preUser.css({
			"background": "#B0C4DE"
		});
	} else {
		preUser.css({
			"background": "#708090"
		});
	}
	isSingleOrGroup = "single";
	preChatUserId = chatUserId;
};

//切换群组聊天窗口div
var chooseGroupDivClick = function(li) {
	var chatGroupId = li.id;
	if ((chatGroupId != curChatGroupId)) {
		if (curChatGroupId == null) {
			createGroupChatDiv(chatGroupId);
		} else {
			showGroupChatDiv(chatGroupId);
			hiddenGroupChatDiv(curChatGroupId);
		}
	} else {
		showGroupChatDiv(chatGroupId);
	}
	/*$('#null-nouser').css({
		"display" : "none"
	});*/
	if(curChatUserId != null){
		hiddenContactChatDiv(curChatUserId);
	}
	curChatGroupId = chatGroupId;
	isSingleOrGroup = "group";
};
	
// 添加对方发送的聊天信息到显示面板
var appendMsgSendByOthers = function(name, message, contact, chattype){
	if(chattype=='single'){
		var contactUL = document.getElementById("contactlistUL");
		if (contactUL.children.length == 0) {
			return null;
		}
		// 
		var contactDivId = name;
		var contactLi = getContactLi(name);
		var date = new Date();
		var time = date.toLocaleTimeString();
		var headstr = [ "<p1>" + name + "   <span></span>" + "   </p1>",
				"<p2>" + time + "<b></b><br/></p2>" ];
		var header = $(headstr.join(''))

		var lineDiv = document.createElement("div");
		var lineDivStyle = document.createAttribute("style");
		lineDivStyle.nodeValue = "margin: 0px 10px 6px 10px;"; 
		lineDiv.setAttributeNode(lineDivStyle); 
		for ( var i = 0; i < header.length; i++) {
			var ele = header[i];
			lineDiv.appendChild(ele);
		}
			
		var eletext = "<p3>" + message + "</p3>";
		var ele = $(eletext);
		ele[0].setAttribute("class", "chat-content-p3");
		ele[0].setAttribute("className", "chat-content-p3");
		ele[0].style.backgroundColor = "#9EB867";
		
		for ( var j = 0; j < ele.length; j++) {
			lineDiv.appendChild(ele[j]);
		}
					
		//if (curChatUserId.indexOf(contact) < 0) {
			contactLi.style.backgroundColor = "#FF4500";
		//}
			 
		var msgContentDiv = getContactChatDiv(contactDivId);
		lineDiv.style.textAlign = "left";
		
		var create = false;
		if (msgContentDiv == null) {
			msgContentDiv = createContactChatDiv(contactDivId);
			create = true;
		}
		msgContentDiv.appendChild(lineDiv);
		if (create) {
			document.getElementById(msgCardDivId).appendChild(msgContentDiv);
		}
		msgContentDiv.scrollTop = msgContentDiv.scrollHeight;
		emojify.run();
		return lineDiv;
	}
	if(chattype=='group'){
		var groupUL = document.getElementById("grouplistUL");
		if (groupUL.children.length == 0) {
			return null;
		}
		// 
		var contactDivId = contact;
		var contactLi = getContactLi(contact);
		var date = new Date();
		var time = date.toLocaleTimeString();
		var headstr = [ "<p1>" + name + "   <span></span>" + "   </p1>",
				"<p2>" + time + "<b></b><br/></p2>" ];
		var header = $(headstr.join(''))

		var lineDiv = document.createElement("div");
		var lineDivStyle = document.createAttribute("style");
		lineDivStyle.nodeValue = "margin: 0px 10px 6px 10px;"; 
		lineDiv.setAttributeNode(lineDivStyle); 
		for ( var i = 0; i < header.length; i++) {
			var ele = header[i];
			lineDiv.appendChild(ele);
		}
			
		var eletext = "<p3>" + message + "</p3>";
		var ele = $(eletext);
		ele[0].setAttribute("class", "chat-content-p3");
		ele[0].setAttribute("className", "chat-content-p3");
		ele[0].style.backgroundColor = "#9EB867";
		
		for ( var j = 0; j < ele.length; j++) {
			lineDiv.appendChild(ele[j]);
		}
					
		//if (curChatGroupId.indexOf(contact) < 0) {
			contactLi.style.backgroundColor = "#FF4500";
		//}
			 
		var msgContentDiv = getGroupChatDiv(contactDivId);
		lineDiv.style.textAlign = "left";
		
		var create = false;
		if (msgContentDiv == null) {
			msgContentDiv = createGroupChatDiv(contactDivId);
			create = true;
		}
		msgContentDiv.appendChild(lineDiv);
		if (create) {
			document.getElementById(msgCardDivId).appendChild(msgContentDiv);
		}
		msgContentDiv.scrollTop = msgContentDiv.scrollHeight;
		emojify.run();
		return lineDiv;
	}
};
	
// 添加自己发送的聊天信息到显示面板
var appendMsgSendByMe = function(message) {
	var date = new Date();
	var time = date.toLocaleTimeString();
	var headstr = [ "<p1> 我 <span></span>" + "   </p1>",
			"<p2>" + time + "<b></b><br/></p2>" ];
	var header = $(headstr.join(''))

	var lineDiv = document.createElement("div");
	var lineDivStyle = document.createAttribute("style");
	lineDivStyle.nodeValue = "margin: 0px 10px 6px 10px;"; 
	lineDiv.setAttributeNode(lineDivStyle); 
	for ( var i = 0; i < header.length; i++) {
		var ele = header[i];
		lineDiv.appendChild(ele);
	}
	
	var eletext = "<p3>" + message + "</p3>";
	var ele = $(eletext);
	ele[0].setAttribute("class", "chat-content-p3");
	ele[0].setAttribute("className", "chat-content-p3");
	ele[0].style.backgroundColor = "#9EB867";
		
	for ( var j = 0; j < ele.length; j++) {
			lineDiv.appendChild(ele[j]);
	}
	
	var msgContentDiv;
	if(isSingleOrGroup=='single'){
		msgContentDiv = getContactChatDiv(curChatUserId); 
	} else if(isSingleOrGroup=='group'){
		msgContentDiv = getGroupChatDiv(curChatGroupId); 
	}
	lineDiv.style.textAlign = "right";
	
	var create = false;
	if (msgContentDiv == null) {
		if(isSingleOrGroup=='single'){
			msgContentDiv = createContactChatDiv(curChatUserId);
		} else {
			msgContentDiv = createGroupChatDiv(curChatUserId);
		}
		create = true;
	}
	msgContentDiv.appendChild(lineDiv);
	if (create) {
		document.getElementById(msgCardDivId).appendChild(msgContentDiv);
	}
	msgContentDiv.scrollTop = msgContentDiv.scrollHeight;
	emojify.run();
	return lineDiv;
}; 

var appendPicMsgSendByMe = function(message) {
	var date = new Date();
	var time = date.toLocaleTimeString();
	var headstr = [ "<p1> 我 <span></span>" + "   </p1>",
			"<p2>" + time + "<b></b><br/></p2>" ];
	var header = $(headstr.join(''))

	var lineDiv = document.createElement("div");
	var lineDivStyle = document.createAttribute("style");
	lineDivStyle.nodeValue = "margin: 0px 10px 6px 10px;"; 
	lineDiv.setAttributeNode(lineDivStyle); 
	for ( var i = 0; i < header.length; i++) {
		var ele = header[i];
		lineDiv.appendChild(ele);
	}
	
	var eletext = "<p3 >" + message + "</p3>";
	var ele = $(eletext);
	ele[0].setAttribute("class", "chat-content-pic");
	ele[0].setAttribute("className", "chat-content-pic");
	ele[0].style.backgroundColor = "#9EB867";
		
	for ( var j = 0; j < ele.length; j++) {
			lineDiv.appendChild(ele[j]);
	}
	
	var msgContentDiv;
	if(isSingleOrGroup=='single'){
		msgContentDiv = getContactChatDiv(curChatUserId); 
	} else if(isSingleOrGroup=='group'){
		msgContentDiv = getGroupChatDiv(curChatGroupId); 
	}
	lineDiv.style.textAlign = "right";
	
	var create = false;
	if (msgContentDiv == null) {
		if(isSingleOrGroup=='single'){
			msgContentDiv = createContactChatDiv(curChatUserId);
		} else {
			msgContentDiv = createGroupChatDiv(curChatUserId);
		}
		create = true;
	}
	msgContentDiv.appendChild(lineDiv);
	if (create) {
		document.getElementById(msgCardDivId).appendChild(msgContentDiv);
	}
	msgContentDiv.scrollTop = msgContentDiv.scrollHeight;
	emojify.run();
	return lineDiv;
}; 
	
//选择联系人的处理
var getContactLi = function(chatUserId) {
	return document.getElementById(chatUserId);
};

var emotionFlag = false;

// emoji 选择框
var showEmotionDialog = function() {
	if (emotionFlag) {
		$('#wl_faces_box').css({
			"display" : "block"
		});
		return;
	}
	emotionFlag = true;
	$('#wl_faces_box').css({
		"display" : "block"
	});
	emojify.run();
};

//表情选择div的关闭方法
var turnoffFaces_box = function() {
	$("#wl_faces_box").fadeOut("quick");
};

//  选择emoji表情
var selectEmotionImg = function(selImg) {
	var origin_content = document.getElementById('talkInputId').value;
	var content = origin_content +' '+ selImg.id;
	document.getElementById('talkInputId').value = content;
	$('#talkInputId').text(content);
	$('#talkInputId').focus();
};

/****/
emojify.setConfig({
    emojify_tag_type : 'img',         
    only_crawl_id    : null,            
    img_dir          : '../../res/img/emoji',  
    ignored_tags     : {               
        'SCRIPT'  : 1,
        'TEXTAREA': 1,
        'A'       : 1,
        'PRE'     : 1,
        'CODE'    : 1
    }
});
emojify.run();
/****/

(function(){
	//设置表情的json数组
	var sjson = new Array(':bowtie:',':smile:',':laughing:',':blush:',':smiley:',':relaxed:',
									':smirk:',':purple_heart:',':heart:',':green_heart:',':broken_heart:',
									':heartbeat:',':heartpulse:',':two_hearts:',':revolving_hearts:',':cupid:',
									':sparkling_heart:',':sparkles:'
	);
	for (var i=0; i<sjson.length; i++) {
		$("<li onclick='selectEmotionImg(this);' id="+sjson[i]+">"+sjson[i]+"</li>").appendTo($('#emotionUL'));
	};
})();

//  清理当前聊天窗口
var clearCurrentChat = function(){
	console.log("clean the chat window.");
}
	
//  发送图片
var picfile;
$('#picfileInput').on('change', function(){
	picfile = this;
});

var sendPicFile = function(){
	var myDate = new Date();
	var mytime = myDate.getTime();
	appendPicMsgSendByMe("<img onclick='zoomOut(this)' id="+mytime+" width='100px;' height='70px;' style='cursor:pointer'></img>");
	preivew(picfile, mytime);
	$('#picfileInput').val('');
}

//  点击浏览原图
var zoomOut = function(obj){
	var src = $(obj).attr('src');
	$('#zoomOutPic').attr('src', src);
	$('#zoomOutPicView').modal('show');
}

//  显示好友列表
var showFriendsList = function(){
	
}
