
/*-------------------第三方调用方式----------------------*/

//  自己的业务需要添加的变量
var uid = null;
var curUserId = null;  //  当前用户id
var curChatUserId = null;  //  当前聊天对象id
var curChatGroupId = null;  // 当前聊天Group id
var curChatUserSessionId  = null;  // websocket connection sessionId
var msgCardDivId = "chat01";
var talkToDivId = "talkTo";
var isSingleOrGroup = "single";  //  标识是单聊还是群聊
var curChatPanelId = null;
var user_name = null;
var uploadToken = null;
var unreadMsgCount = 0;

//   建立连接
JPushIM.connect();

//初始化IM业务配置
JPushIM.init({
	appKey : '521c83e1ac1d4c4800961540',
	secrect : 'master secrect',
	onConnect : function(){
		connectResp();
	},
	onLogin : function(data){
		loginResp(data);
	},
	onGetUploadToken : function(data){
		getUploadTokenResp(data);
	},
	onGetContracterList : function(data){
		getContracterListResp(data);
	},
	onGetGroupsList : function(data){
		getGroupsList(data);
	},
	onChatEvent : function(data){
		chatEventResp(data);
	},
	onDisConnect : function(data){
		disconnectResp(data);
	},
	onIMException : function(data){
		// 开发者自定义异常处理，若不定义则采用系统默认处理方式
	}
});

/*------------------自己的业务逻辑处理--------------------*/

//  连接响应处理
var connectResp = function(){
	console.log('连接建立成功！');
};

//  登陆响应处理
var loginResp = function(data){
	console.log('处理登陆响应！');
	if(data!=null && data.uid!=0){
		uid = data.uid;
		curUserId = uid;
	} else {
		$('#waitLoginmodal').css({"display":"none"});
		alert('登陆失败，可能您的帐号不对.');
		location.reload();   //  重新加载
		return;
	}
	createConversionlistUL();  //  创建会话列表
	var options = {
			'userName' : user_name
	};
	JPushIM.getContracterListEvent(options);
	options = {
			'uid' : uid
	};
	JPushIM.getGroupListEvent(options);
	$('#waitLoginmodal').css({"display":"none"});
	$('#content').css({"display":"block"});
};

//上传多媒体文件时的响应函数
var filesAddedFunc = function(files){
	plupload.each(files, function(file) {
 	   //console.log(file);
 	   $('#picFileModal').modal('hide');
	});
};

var uploadProgressFunc = function(progress){
	console.log('上传文件进度： '+progress);
	//  添加自己想要的上传进度处理方式
};

var uploadCompleteFunc = function(src){
  	appendPicMsgSendByMe("<img onclick='zoomOut(this)' src="+ src +" width='100px;' height='70px;' style='cursor:pointer'></img>");
  	var toUserName = $('#'+curChatUserId).attr('username');
  	var message =  JPushIM.buildMessageContent("single", "image", curChatUserId, toUserName,
							uid, user_name, "time..", src);
   JPushIM.chatEvent(message);
};
//  end 上传多媒体文件处理

//  上传token处理
var getUploadTokenResp = function(data){
	$('#picFileModal').modal('show');
	uploadToken = data;
	JPushIM.uploadMediaFile(uid, uploadToken, 'fileContainer', 'fileChooseBtn', filesAddedFunc, uploadProgressFunc, uploadCompleteFunc);
};

//   联系人响应处理
var getContracterListResp = function(data){
	console.log('处理获取联系人响应！');
	createContactlistUL();  // 创建联系人列表UI
	var uielem = document.getElementById("contactlistUL");
	for (i = 0; i < data.length; i++) {
		var lielem = document.createElement("li");
		$(lielem).attr({
			'id' : data[i].uid,
			'username' : data[i].username,
			'class' : 'online',
			'className' : 'online',
			'onclick': 'chooseContactDivClick(this)',
			'chat' : 'chat',
			'displayName' : data[i].username
		});
		var imgelem = document.createElement("img");
		imgelem.setAttribute("src", "../../res/img/head/contact_normal.png");
		imgelem.setAttribute("style", "border-radius: 50%;");
		
		var unreadelem = document.createElement("img");
		unreadelem.setAttribute("src", "../../res/img/msg_unread.png");
		unreadelem.setAttribute("class", "unread");
		unreadelem.setAttribute("style", "visibility:hidden");
		lielem.appendChild(imgelem);
		lielem.appendChild(unreadelem);

		var spanelem = document.createElement("span");
		$(spanelem).attr({
			"class" : "contractor-display-style"
		});
		spanelem.innerHTML = data[i].username;
		
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
		setCurrentContact(data[0].uid);
	}
};

//  获取群组处理
var getGroupsList = function(data){
	console.log('处理群组响应！');
	createGroupslistUL();  // 创建群组列表UI
	var uielem = document.getElementById("grouplistUL");
	for (i = 0; i < data.length; i++) {
		var lielem = document.createElement("li");
		$(lielem).attr({
			'id' : data[i].gid,
			'chat' : 'chat',
			'onclick': 'chooseGroupDivClick(this)',
			'displayName' : data[i].group_name,
		});
		
		var imgelem = document.createElement("img");
		imgelem.setAttribute("src", "../../res/img/head/group_normal.png");
		imgelem.setAttribute("style", "border-radius: 50%;");
		
		var unreadelem = document.createElement("img");
		unreadelem.setAttribute("src", "../../res/img/msg_unread.png");
		unreadelem.setAttribute("class", "unread");
		unreadelem.setAttribute("style", "visibility:hidden");
		lielem.appendChild(imgelem);
		lielem.appendChild(unreadelem);
		
		var spanelem = document.createElement("span");
		$(spanelem).attr({
			"class" : "contractor-display-style"
		});
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
	if(data.length>0)
		curChatGroupId = data[0].gid;
};

//  聊天消息处理
var chatEventResp = function(data){
	updateAddUnreadMsgInfo();
	appendMsgSendByOthers(data.userName, data.message, data.toUserName, data.msgType, data.contentType);
};

//  网络断开处理
var disconnectResp = function(data){
	console.log('网络断开处理.');
	alert('您已经与服务器断开，请重新连接.');
};

//   自定义相关的业务逻辑函数
//   绑定用户登陆处理
$('#login_submit').click(function(){
	console.log('user login submit.');
	$('#loginPanel').css({"display":"none"});
	$('#waitLoginmodal').css({"display":"block"}); 
	var message = 'user:'+user_name+'login';
	user_name = $('#user_name').val();
	var password = $('#password').val();
	var options = {
			'userName' : user_name,
			'password' : password
	};
	JPushIM.sendLoginEvent(options);
});


var showChooseFileDialog = function(){
	//  获取图片上传token
	JPushIM.getUploadTokenEvent();
}

	
//  左边栏仿tab处理
$('#conversionTab').click(function(){
	/*$("#conversionlist").css({
		display:"block"
	});*/
	$("#conversionlist").fadeIn('normal');
	$("#contractlist").css({
		display:"none"
	});
	$('#grouplist').css({
		display:"none"
	});
	$(this).parent().attr('class', 'active');
	$("#friendsTab").parent().attr('class', '');
	$('#groupsTab').parent().attr('class', '');
});
$('#friendsTab').click(function(){
	/*$("#contractlist").css({
		display:"block"
	});*/
	$("#contractlist").fadeIn('normal');
	$("#conversionlist").css({
		display:"none"
	});
	$('#grouplist').css({
		display:"none"
	});
	$(this).parent().attr('class', 'active');
	$("#conversionTab").parent().attr('class', '');
	$('#groupsTab').parent().attr('class', '');
});

$('#groupsTab').click(function(){
	/*$('#grouplist').css({
		display:"block"
	});*/
	$('#grouplist').fadeIn('normal');
	$("#conversionlist").css({
		display:"none"
	});
	$("#contractlist").css({
		display:"none"
	});
	$(this).parent().attr('class', 'active');
	$("#conversionTab").parent().attr('class', '');
	$('#friendsTab').parent().attr('class', '');
});

//  发起聊天 好友选择列表切换
var showFriendsListPanel = function(obj){
	$(obj).parent().attr('class', 'active');
	$(obj).parent().siblings().attr('class', '');
	$('#startChatGroupsList').css({
		display: "none"
	});
	$('#startChatFriendsList').css({
		display: "block"
	});
}

//  发起聊天  群组选择列表切换
var showGroupsListPanel = function(obj){
	$(obj).parent().attr('class', 'active');
	$(obj).parent().siblings().attr('class', '');
	$('#startChatFriendsList').css({
		display: "none"
	});
	$('#startChatGroupsList').css({
		display: "block"
	});
}

//  回车键发送消息
function stopDefault(e) {  
    if(e && e.preventDefault) {  
    	e.preventDefault();  
    } else {  
    	window.event.returnValue = false;   
    }  
    return false;  
}  

//  监听用户按键，处理回车键事件
document.onkeydown = function(event){
	var e = event || window.event || arguments.callee.caller.arguments[0];      
   if(e && e.keyCode==13){   // Enter按键
	   stopDefault(e);
	   var content = document.getElementById('talkInputId').value;
	   if(content!=''){
		   appendMsgSendByMe(content);
		   var toUserName = $('#'+curChatUserId).attr('username');
		   if(isSingleOrGroup=='single'){
			   addToConversionList(curChatUserId);  //   添加该会话到会话列表
			   updateConversionRectMsg(curChatUserId, content);
			   var message =  JPushIM.buildMessageContent("single", "text", curChatUserId, toUserName,
					   								uid, user_name, "time..", content);
			   JPushIM.chatEvent(message);
		   } else if(isSingleOrGroup=='group'){
			   addToConversionList(curChatGroupId);  //   添加该会话到会话列表
			   updateConversionRectMsg(curChatGroupId, content);
			   var message =  JPushIM.buildMessageContent("group", "text", curChatGroupId, toUserName,
							uid, user_name, "time..", content);
			   JPushIM.chatEvent(message);
		    } 
	   } else {
		   alert('您还未输入.');
	   }
	   document.getElementById('talkInputId').value = '';
    }
}; 

//  用户发送消息
function sendText(){
	 var content = document.getElementById('talkInputId').value;
	 if(content==''){
		  alert('您还未输入.');
		  return;
	  } 
 	 appendMsgSendByMe(content);
    document.getElementById('talkInputId').value = '';
    var toUserName = $('#'+curChatUserId).attr('username');
    if(isSingleOrGroup=='single'){
    	addToConversionList(curChatUserId);  //   添加该会话到会话列表
    	updateConversionRectMsg(curChatUserId, content);
    	var message =  JPushIM.buildMessageContent("single", "text", curChatUserId, toUserName,
					uid, user_name, "time..", content);	
    	JPushIM.chatEvent(message);
    } else if(isSingleOrGroup=='group'){
    	addToConversionList(curChatGroupId);  //   添加该会话到会话列表
    	updateConversionRectMsg(curChatGroupId, content);
    	var message = JPushIM.buildMessageContent("group", "text", curChatGroupId, toUserName,
				uid, user_name, "time..", content);
    	JPushIM.chatEvent(message);
     } 
};
	 
//   添加会话到会话列表中
var addToConversionList = function(id){
	$('#conversionlist').css('background-image', '');
	var length = $("#conversionlistUL li").length;
	for(var i=0; i<length; i++){  //  检查是否添加重复项
		 var lielem = $("#conversionlistUL li")[i];
		 var lid = $(lielem).attr('id');
		 if(lid=='conversion-'+id)
			 return;
	}
	
	var node = $('#'+id).clone(true);
	var newId = 'conversion-'+$(node).attr('id');
	$(node).attr('id', newId);
	$(node).attr('unreadcount', 0);
	//  添加显示最近的消息
	var recr_msg_elem = document.createElement("span");
	$(recr_msg_elem).attr({
		'id' : id+'-rect-msg',
		"class" : "rect-msg-display-style"
	});
	
	/*if(isSingleOrGroup=='single'){
		node.onclick = function(){
			chooseConversionSingleDivClick(this);
		};
	} else if(isSingleOrGroup=='group'){
		node.onclick = function(){
			chooseConversionGroupDivClick(this);
		};
	}*/
	$('#conversionlistUL').prepend(node);
	$('#'+newId).append(recr_msg_elem);
}

//  更新会话列表中的最近消息状态
var updateConversionRectMsg = function(id, msg){
	$('#'+id+'-rect-msg').html(msg);
	//emojify.run();
}


//  获取当前聊天记录的窗口div
var getContactChatDiv = function(chatUserId) {
	return document.getElementById(curUserId + "-" + chatUserId);
};

//  获取当前群组聊天记录的窗口
var getGroupChatDiv = function(chatGroupId) {
	return document.getElementById(curUserId + "-" + chatGroupId);
};
	
//  创建一个联系人聊天窗口
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
	
//  创建联系人列表UI
var createContactlistUL = function() {
	var uielem = document.createElement("ul");
	$(uielem).attr({
		"id" : "contactlistUL",
		"class" : "chat03_content_ul"
	});
	var contactlist = document.getElementById("contractlist");
	contactlist.appendChild(uielem);
};

//  创建群组列表UI
var createGroupslistUL = function() {
	var uielem = document.createElement("ul");
	$(uielem).attr({
		"id" : "grouplistUL",
		"class" : "chat03_content_ul"
	});
	var contactlist = document.getElementById("grouplist");
	contactlist.appendChild(uielem);
};

//   创建会话列表UI
var createConversionlistUL = function() {
	var uielem = document.createElement("ul");
	$(uielem).attr({
		"id" : "conversionlistUL",
		"class" : "chat03_content_ul"
	});
	var contactlist = document.getElementById("conversionlist");
	contactlist.appendChild(uielem);
};
	
//  设置当前联系人界面
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
	
};
	
//  显示与联系人聊天的窗口
var showContactChatDiv = function(chatUserId) {
	var contentDiv = getContactChatDiv(chatUserId);
	if (contentDiv == null) {
		contentDiv = createContactChatDiv(chatUserId);
		document.getElementById(msgCardDivId).appendChild(contentDiv);
	}
	contentDiv.style.display = "block";
	var contactLi = document.getElementById(chatUserId);
	var conversionLi = document.getElementById('conversion-'+chatUserId);
	//var contactLi = $('[id='+chatUserId+']');
	if(contactLi == null){
		return;
	}
	if(conversionLi!=null){
		conversionLi.style.backgroundColor = "#B0E0E6";
	}
	contactLi.style.backgroundColor = "#B0E0E6";
	//contactLi.css({'background-color':'#B0E0E6'});
	var chatName = $('#'+chatUserId).attr('username');
	var dispalyTitle = "与 " + chatName + " 聊天中";
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
	var conversionLi = document.getElementById('conversion-'+chatGroupId);
	//var contactLi = $('[id='+chatGroupId+']');
	if(contactLi==null){
		return;
	}
	if(conversionLi){
		conversionLi.style.backgroundColor = "#B0E0E6";
	}
	contactLi.style.backgroundColor = "#B0E0E6";
	//contactLi.css({'background-color':'#B0E0E6'});
	var group_name = $('#'+chatGroupId).attr("displayName");
	var dispalyTitle = "正在 " + group_name + " 群里聊天中";
	document.getElementById(talkToDivId).children[0].innerHTML = dispalyTitle;
};
	
//对上一个联系人的聊天窗口做隐藏处理
var hiddenContactChatDiv = function(chatUserId) {
	var contactLi = document.getElementById(chatUserId);
	var conversionLi = document.getElementById('conversion-'+chatUserId);
	//var contactLi = $('[id='+chatUserId+']');
	if (contactLi) {
		contactLi.style.backgroundColor = "";
		//contactLi.css({'background-color':''});
	}
	if(conversionLi){
		conversionLi.style.backgroundColor = "";
	}
	var contentDiv = getContactChatDiv(chatUserId);
	if (contentDiv) {
		contentDiv.style.display = "none";
	}
};

//对上一个群组的聊天窗口做隐藏处理
var hiddenGroupChatDiv = function(chatGroupId) {
	var contactLi = document.getElementById(chatGroupId);
	var conversionLi = document.getElementById('conversion-'+chatGroupId);
	if (contactLi) {
		contactLi.style.backgroundColor = "";
	}
	if(conversionLi){
		conversionLi.style.backgroundColor = "";
	}
	var contentDiv = getGroupChatDiv(chatGroupId);
	if (contentDiv) {
		contentDiv.style.display = "none";
	}
};

// 切换联系人聊天窗口div
var chooseContactDivClick = function(li) {
	hideGroupInfoPanel();
	var chatUserId = li.id;
	var indexPos = chatUserId.indexOf("conversion");
	if(indexPos!=-1){
		chatUserId = li.id.split('-')[1];
		hideUnreadMsgMark(chatUserId);
	}
	//curChatUserSessionId = li.sessionId;
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
	$('#null-nouser').css({
		"display" : "none"
	});	
	isSingleOrGroup = "single";
};

//  切换群组聊天窗口div
var chooseGroupDivClick = function(li) {
	var chatGroupId = li.id;
	var indexPos = chatGroupId.indexOf("conversion");
	if(indexPos!=-1){
		chatGroupId = li.id.split('-')[1];
		hideUnreadMsgMark(chatGroupId);
	}
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
	$('#null-nouser').css({
		"display" : "none"
	});
	if(curChatUserId != null){
		hiddenContactChatDiv(curChatUserId);
	}
	curChatGroupId = chatGroupId;
	isSingleOrGroup = "group";
};
	 
//   添加对方发送的聊天信息到显示面板
var appendMsgSendByOthers = function(name, message, contact, chattype, contentType){
	if(chattype=='single'){
		var contactUL = document.getElementById("contactlistUL");
		if (contactUL.children.length == 0) {
			return null;
		}
		var contactDivId = name;
		var contactLi = getContactLi('conversion-'+name);
		if(contactLi==null){
			addToConversionList(contactDivId);
			contactLi = getContactLi('conversion-'+name);
		}
		var date = new Date();
		var time = date.toLocaleTimeString();
		var headstr = [ "<p1>" + $('#'+name).attr('username') + "   <span></span>" + "   </p1>",
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
		
		var ele;
		if('text'==contentType){
			var eletext = "<p3>" + message + "</p3>";
			ele = $(eletext);
			ele[0].setAttribute("class", "chat-content-p3");
			ele[0].setAttribute("className", "chat-content-p3");
			ele[0].style.backgroundColor = "#9EB867";
			updateConversionRectMsg(contactDivId, message);  //  更新会话列表中最新的消息
		} else if('image'==contentType){
			message = "<img onclick='zoomOut(this)' src="+message+" width='100px;' height='70px;' style='cursor:pointer'></img>";
			var eletext = "<p3 >" + message + "</p3>";
			ele = $(eletext);
			ele[0].setAttribute("class", "chat-content-pic");
			ele[0].setAttribute("className", "chat-content-pic");
			ele[0].style.backgroundColor = "#9EB867";
			updateConversionRectMsg(contactDivId, "图片文件");  //  更新会话列表中最新的消息
		}
		
		
		for ( var j = 0; j < ele.length; j++) {
			lineDiv.appendChild(ele[j]);
		}
					
		//if (curChatUserId.indexOf(contact) < 0) {
			showUnreadMsgMark(name);
			//contactLi.style.backgroundColor = "#FF4500";
			//contactLi.css({'background-color':'#FF4500'});
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
		//emojify.run();
		return lineDiv;
	}
	if(chattype=='group'){
		var groupUL = document.getElementById("grouplistUL");
		if (groupUL.children.length == 0) {
			return null;
		}
		
		var contactDivId = contact;
		var contactLi = getContactLi('conversion-'+contact);
		if(contactLi==null){
			addToConversionList(contactDivId);
			contactLi = getContactLi('conversion-'+name);
		}
		var date = new Date();
		var time = date.toLocaleTimeString();
		var headstr = [ "<p1>" + $('#'+name).attr('displayname') + "   <span></span>" + "   </p1>",
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
			
		var ele;
		if('text'==contentType){
			var eletext = "<p3>" + message + "</p3>";
			ele = $(eletext);
			ele[0].setAttribute("class", "chat-content-p3");
			ele[0].setAttribute("className", "chat-content-p3");
			ele[0].style.backgroundColor = "#9EB867";
			updateConversionRectMsg(contactDivId, message);  //  更新会话列表中最新的消息
		} else if('image'==contentType){
			message = "<img onclick='zoomOut(this)' src="+message+" width='100px;' height='70px;' style='cursor:pointer'></img>";
			var eletext = "<p3 >" + message + "</p3>";
			ele = $(eletext);
			ele[0].setAttribute("class", "chat-content-pic");
			ele[0].setAttribute("className", "chat-content-pic");
			ele[0].style.backgroundColor = "#9EB867";
			updateConversionRectMsg(contactDivId, "图片文件");  //  更新会话列表中最新的消息
		}
		
		for ( var j = 0; j < ele.length; j++) {
			lineDiv.appendChild(ele[j]);
		}
					
		//if (curChatGroupId.indexOf(contact) < 0) {
			showUnreadMsgMark(name);
			//contactLi.style.backgroundColor = "#FF4500";
			//contactLi.css({'background-color':'#FF4500'});
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
		//emojify.run();
		return lineDiv;
	}
};

//  隐藏群组信息面板
var hideGroupInfoPanel = function(){
	$('#groupInfo').css('display', 'none');
	$('#chat01.chat01').slideDown();
	$('.chat02').slideDown();
}
	
//  显示未读消息标记
var showUnreadMsgMark = function(id){
	$('#conversionlist'+' li#conversion-'+id+' img.unread').css("visibility","visible");
	var count = +$('#conversionlist'+' li#conversion-'+id).attr('unreadcount');
	$('#conversionlist'+' li#conversion-'+id).attr('unreadcount', ++count);
}

//	取消未读消息标记
var hideUnreadMsgMark = function(id){
	$('#conversionlist'+' li#conversion-'+id+' img.unread').css("visibility","hidden");
	var count = +$('#conversionlist'+' li#conversion-'+id).attr('unreadcount');
	updateReduceUnreadMsgInfo(count);
	$('#conversionlist'+' li#conversion-'+id).attr('unreadcount', 0);
}

//  添加自己发送的聊天信息到显示面板
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
	//emojify.run();
	return lineDiv;
}; 

//  添加自己发送的图片消息到消息面板
var appendPicMsgSendByMe = function(message) {
	var date = new Date();
	var time = date.toLocaleTimeString();
	var headstr = [ "<p1> 我 <span></span>" + "   </p1>",
			"<p2>" + time + "<b></b><br/></p2>" ];
	var header = $(headstr.join(''));

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
		updateConversionRectMsg(curChatUserId, "图片文件");
	} else if(isSingleOrGroup=='group'){
		msgContentDiv = getGroupChatDiv(curChatGroupId); 
		updateConversionRectMsg(curChatGroupId, "图片文件");
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
	//emojify.run();
	return lineDiv;
};
 	
//  选择联系人的处理
var getContactLi = function(chatUserId) {
	return document.getElementById(chatUserId);
	//return $('[id='+chatUserId+']');
};

//  emoji 选择框
var showEmotionDialog = function() {
	$('#wl_faces_box').css({
		"display" : "block"
	});
	//emojify.run();
};

//  表情选择div的关闭方法
var turnoffFaces_box = function() {
	$("#wl_faces_box").css({'display':'none'});
};

//  选择emoji表情
var selectEmotionImg = function(selImg) {
	var origin_content = document.getElementById('talkInputId').value;
	var content = origin_content +' '+ selImg.id;
	document.getElementById('talkInputId').value = content;
	$('#talkInputId').text(content);
	$('#talkInputId').focus();
};

//  初始化 emoji 和 动画表情面板 
JPushIM.initEmojiPanelOne('#emotionUL');
JPushIM.initEmojiPanelTwo('#carton_emotionUL');

//  清理当前聊天窗口
var clearCurrentChat = function(){
	console.log("clean the chat window.");
}
	
//  选择图片
var picfile;
$('#file').on('change', function(){
	picfile = this;
});

//  发送图片消息
/*var sendPicFile = function(){
	var myDate = new Date();
	var mytime = myDate.getTime();
	appendPicMsgSendByMe("<img onclick='zoomOut(this)' id="+mytime+" width='100px;' height='70px;' style='cursor:pointer'></img>");
	preivew(picfile, mytime);
	//$('#file').val('');
}*/

//  点击浏览原图
var zoomOut = function(obj){
	var src = $(obj).attr('src');
	var origin_src = src.split('?')[0];
	$('#zoomOutPic').attr('src', origin_src);
	$('#zoomOutPicView').modal('show');
}

//  查看群成员,显示群成员面板
var showGroupMembers = function(){
	$('#chat01.chat01').slideUp();
	$('.chat02').slideUp();
	$('#groupInfo').slideDown();
}
 
//  隐藏群成员信息，显示之前的聊天面板
var backToChat = function(){
	$('#groupInfo').slideUp();
	$('#chat01.chat01').slideDown();
	$('.chat02').slideDown();
}

//  添加好友
var addNewFriends = function(){
	$('#addNewFriendModal').modal('show');
}

//  发送添加好友请求
var sendAddFriendCmd = function(){
// add friend logic
}

//  发起聊天
var startNewChat = function(){
	$('#startNewChatModal').modal('show');
}

// 发送发起聊天请求
var sendStartNewChatCmd = function(){
	
}

//  添加群组新成员
var addNewMember = function(){
	$('#addNewGroupMembersModal').modal('show');
}

//  显示群成员信息
var showMemberInfo = function(){
	$('#showGroupMemberDetailInfoModal').modal('show');
}

//  更新未读消息数显示(增加未读消息数量)
var updateAddUnreadMsgInfo = function(){
	$('.badge').html(++unreadMsgCount);
	notifyUnreadMsg();
}

//  更新未读消息数显示(减少未读消息数量)
var updateReduceUnreadMsgInfo = function(count){
	unreadMsgCount -= count;
	if(unreadMsgCount != 0){
		$('.badge').html(unreadMsgCount);
	} else {
		$('.badge').html('');
	}
}

//  未读消息滚动提示
var step=0;
var _title=document.title;
var intervalId;
var flashTitle = function(){
	if(unreadMsgCount==0){
		clearInterval(intervalId);
		document.title=_title;
		return;
	}
	var space='【有'+unreadMsgCount+'条未读消息】';
	step++;  
	if (step==3) {step=1}  
	if (step==1) {document.title=space}  
	if (step==2) {document.title=_title} 
}

//  消息提示
var notifyUnreadMsg = function(){ 
	intervalId = setInterval("flashTitle()",1500);
}

//  表情面板切换---> 符号表情
var changeSimbolPanel = function(){
	$('#wl_faces_main_simpol').css({
		'display':'block'
	});
	$('.title_name_simbol').css({
		'background':'#A0A0A0'
	});
	$('.title_name_carton').css({
		'background':''
	});
	$('#wl_faces_main_carton').css({
		'display':'none'
	});
}

//  表情面板切换---> 卡通表情
var changeCartonPanel = function(){
	$('#wl_faces_main_carton').css({
		'display':'block'
	});
	$('.title_name_carton').css({
		'background':'#A0A0A0'
	});
	$('.title_name_simbol').css({
		'background':''
	});
	$('#wl_faces_main_simpol').css({
		'display':'none'
	});
}

