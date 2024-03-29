
/*-------------------第三方调用方式----------------------*/

//  自己的业务需要添加的变量
var uid = null;
var juid = null;
var sid = null;
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

var imageContent = {
	height:"",
	width:"",
	media_crc32:"",
	duration:"",
	media_id:"",
	content:""
}; 

//   建立连接
JPushIM.connect();

//初始化IM业务配置
JPushIM.init({
	appKey : 'ab5e82b23ee58621a01de671',
	//appKey : '4f7aef34fb361292c566a1cd',
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
	onGetUploadPicMetaInfo : function(data){
		getUploadPicMetaInfoResp(data);
	},
	onGetContracterList : function(data){ 
		getContracterListResp(data);
	},                          
	onGetGroupsList : function(data){
		getGroupsListResp(data);
	},
	onGetGroupMemberList : function(data){
		getGroupMemberListResp(data);
	},
	onChatEvent : function(data){
		chatEventResp(data);
	},
	onMsgSyncEvent : function(data){
		msgSyncResp(data);
	},
	onAddFriendCmd : function(data){
		addFriendCmdResp(data);
	},
	onEventNotification : function(data){
		eventNotificationResp(data);
	},
	onLogout : function(data){
		logoutResp(data);
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
		sid = data.sid;
		juid = data.juid;
		curUserId = uid;
	} else {
		$('#waitLoginmodal').css({"display":"none"});
		alert('登陆失败，可能您的帐号不对.');
		location.reload(); 
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
	$("body").eq(0).css({
		"background-image":"url('../../res/img/bg/imbg001.png')",
		"background-attachment":"fixed",
		"background-repeat":"no-repeat",
		"background-size":"cover",
		"-moz-background-size":"cover",
		"-webkit-background-size":"cover"
	});
	$('#waitLoginmodal').css({"display":"none"});
	$('#content').css({"display":"block"});
};

//上传多媒体文件时的响应函数
var filesAddedFunc = function(files){
	$('#picFileModal').modal('hide');
	plupload.each(files, function(file) {
 	   //console.log(file);
	});
};

var uploadProgressFunc = function(progress){
	console.log('上传文件进度： '+progress);
	//  添加自己想要的上传进度处理方式
};

var uploadCompleteFunc = function(mediaId){
	imageContent.media_id = "/" + mediaId;
	var src = JPushIM.QiNiuMediaUrl + "/" + mediaId + '?imageView2/2/h/100';
  	appendPicMsgSendByMe("<img onclick='zoomOut(this)' src="+ src +" width='100px;' height='70px;' style='cursor:pointer'></img>");
  	JPushIM.getUploadPicMetaInfo(JPushIM.QiNiuMediaUrl + "/" + mediaId);
};
//  end 上传多媒体文件处理

//  上传token处理
var getUploadTokenResp = function(data){
	$('#picFileModal').modal('show');
	uploadToken = data;
	JPushIM.uploadMediaFile(uid, uploadToken, 'fileContainer', 'fileChooseBtn', /*filesAddedFunc,*/ uploadProgressFunc, uploadCompleteFunc);
};

//  获取上传的图片的元信息
var getUploadPicMetaInfoResp = function(data){
	data = JSON.parse(data);
	imageContent.height = data.height;
	imageContent.width = data.width;
	imageContent.media_crc32 = data.crc32;
	var toUserName = $('#'+curChatUserId).attr('username');
	if(isSingleOrGroup=='single'){
  		var message =  JPushIM.buildMessageContent(juid, sid,"single", "image", toUserName, curChatUserId,
					uid, user_name, Date.parse(new Date())/1000, imageContent);
  		JPushIM.chatEvent(message);
  	} else if(isSingleOrGroup=='group'){
  		var message =  JPushIM.buildMessageContent(juid, sid, "group", "image", curChatGroupId, curChatGroupId,
				uid, user_name, Date.parse(new Date())/1000, imageContent);
  		JPushIM.chatEvent(message);
  	}
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
var getGroupsListResp = function(data){
	console.log('处理群组响应！');
	createGroupslistUL();  // 创建群组列表UI
	var uielem = document.getElementById("grouplistUL");
	for (i = 0; i < data.length; i++) {
		var lielem = document.createElement("li");
		$(lielem).attr({
			'id' : data[i].gid,
			'chat' : 'chat',
			'onclick': 'chooseGroupDivClick(this)',
			'displayName' : data[i].name,
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
		spanelem.innerHTML = data[i].name;
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

//  获取群组成员响应
var getGroupMemberListResp = function(data){
	var data = JSON.parse(data);
	if(data=='false'){
		alert('获取群组成员失败');
	} else {
		var memberList = $('#groupMemberInfoListUL li');
		for(var i=0; i<memberList.length-1; i++){
			$(memberList[i]).remove();
		}
		var length = data.length;
		for(var i=0; i<length; i++){
			var uid = data[i].uid;
			var username = data[i].username;
			var ele = "<li id="+ uid + " onmouseover='showDelMemMark(this);' onmouseout='hideDelMemMark(this);' >" +
							"<img id="+ uid +" class='del-mark' src='../../res/img/head/del.png' onclick='delGroupMember(this);' />" +
							"<img id="+ uid +" onclick='showMemberInfo(this);' class='avator' src='../../res/img/head/header2.jpg'/>" +
							"<p class='profileName'>"+username+"</p></li>";
			 $('#addNewMember').before(ele);
		}
	}
};

//  删除群成员时 UI 标记
var showDelMemMark = function(dom){
	var id = $(dom).attr('id');
	$('img[class="del-mark"][id='+id+']').css({"display":"block"}); 
};

var hideDelMemMark = function(dom){
	var id = $(dom).attr('id');
	$('img[class="del-mark"][id='+id+']').css({'display':'none'});
};

//  删除群成员
var delGroupMember = function(dom){
	var toUid = $(dom).attr('id');
	JPushIM.delGroupMemberEvent({
		sid: sid,
		juid: juid,
		uid: uid,
		toUid: toUid, 
		gid: curChatGroupId,
		member_count: 1,
	});
}

//  聊天消息处理
var chatEventResp = function(data){
	updateAddUnreadMsgInfo();
	appendMsgSendByOthers(data.userName, data.message, data.toUserName, data.msgType, data.contentType);
	JPushIM.chatMsgSyncResp({
		uid : curUserId,
		juid : juid,
		sid : sid,
		messageId : data.messageId,
		iMsgType : data.iMsgType
	});
};

//  消息下发处理
var msgSyncResp = function(data){
	updateAddUnreadMsgInfo();
	appendMsgSendByOthers(data.userName, data.message, data.toUserName, data.msgType, data.contentType);
	JPushIM.chatMsgSyncResp({
		uid : curUserId,
		juid : juid,
		sid : sid,
		messageId : data.messageId,
		iMsgType : data.iMsgType
	});
};

//  处理添加好友请求的响应
var addFriendCmdResp = function(data){
	// to do
};

//  事件通知响应处理
var eventNotificationResp = function(data){
	data = jQuery.parseJSON(data);
	
	var eventId = data.eventId;
	var eventType = data.eventType;
	JPushIM.eventSyncResp({
		uid : curUserId,
		juid : juid,
		sid : sid,
		eventId : eventId,
		eventType : eventType
	});
	
	var eventType = data.eventType;
	var gid = data.gid;
	var message = "";
	var domList = $('div.chat01_content');
	var length = domList.length;
	if(eventType==10){
		message = data.message;
	} else if(eventType==9 || eventType==11){
		var toUid = data.toUid;
		var name = $('#groupMemberInfoListUL li#'+toUid+' .profileName').html();
		message = name+' 退出群聊';
		$('groupMemberInfoListUL li#'+toUid).remove();
	}
	for(var i=0; i<length; i++){
		var id = $(domList[i]).attr('id');
		var tid = id.split('-')[1];
		if(tid==gid){
			var lineDiv = document.createElement("div");
			var lineDivStyle = document.createAttribute("style");
			lineDivStyle.nodeValue = "margin: 0px 10px 6px 10px;"; 
			lineDiv.setAttributeNode(lineDivStyle); 
			var eletext = "<p3 >" + message + "</p3>";
			var ele = $(eletext);
			ele[0].setAttribute("class", "chat-content-event");
			ele[0].setAttribute("className", "chat-content-event");
			ele[0].style.backgroundColor = "#E8EAED";
			
			for ( var j = 0; j < ele.length; j++) {
				lineDiv.appendChild(ele[j]);
			}
				
			var msgContentDiv = document.getElementById(id);
			lineDiv.style.textAlign = "center";
			msgContentDiv.appendChild(lineDiv);	
			msgContentDiv.scrollTop = msgContentDiv.scrollHeight;
		}
	}
};

//  网络断开处理
var disconnectResp = function(data){
	alert('您已经与服务器断开，请重新连接.');
};

//  用户手动退出响应
var logoutResp = function(data){
	if(data=='true'){
		alert('成功退出');
	}
};

//   自定义相关的业务逻辑函数
//   绑定用户登陆处理
$('#login_submit').click(function(){
	console.log('user login submit.');
	$('#loginPanel').css({"display":"none"});
	document.body.style.backgroundImage = '';
	document.body.style.backgroundColor = '#FFFFFF';
	$('#waitLoginmodal').css({"display":"block"}); 
	var message = 'user:'+user_name+'login';
	user_name = $('#user_name').val();
	var password = $('#password').val();
	var options = {
			'appKey' : JPushIM.init.appKey,
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
};

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
};

//  回车键发送消息
function stopDefault(e) {  
    if(e && e.preventDefault) {  
    	e.preventDefault();  
    } else {  
    	window.event.returnValue = false;   
    }  
    return false;  
}; 

//  监听用户按键，处理回车键事件
document.onkeydown = function(event){
	var e = event || window.event || arguments.callee.caller.arguments[0];      
   if(e && e.keyCode==13){   // Enter按键
	   stopDefault(e);
	   var content = document.getElementById('talkInputId').value;
	   if(content!=''){
		   var msg_body = {
				   content: content
		    };
		   appendMsgSendByMe(content);
		   if(isSingleOrGroup=='single'){
			   var toUserName = $('#'+curChatUserId).attr('username');
			   addToConversionList(curChatUserId);  //   添加该会话到会话列表
			   updateConversionRectMsg(curChatUserId, content);
			   var message =  JPushIM.buildMessageContent(juid, sid,"single", "text", toUserName, curChatUserId,
					   								uid, user_name, Date.parse(new Date())/1000, msg_body);
			   JPushIM.chatEvent(message);
		   } else if(isSingleOrGroup=='group'){
			   var toGroupName = $('#'+curChatGroupId).attr('displayname');
			   addToConversionList(curChatGroupId);  //   添加该会话到会话列表
			   updateConversionRectMsg(curChatGroupId, content);
			   var message =  JPushIM.buildMessageContent(juid, sid, "group", "text", curChatGroupId, toGroupName,
							uid, curChatGroupId, Date.parse(new Date())/1000, msg_body);
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
	 var msg_body = {
 			content: content
	 };
	 if(content==''){
		  alert('您还未输入.');
		  return;
	  } 
 	 appendMsgSendByMe(content);
    document.getElementById('talkInputId').value = '';
    if(isSingleOrGroup=='single'){
    	var toUserName = $('#'+curChatUserId).attr('username');
    	addToConversionList(curChatUserId);  //   添加该会话到会话列表
    	updateConversionRectMsg(curChatUserId, content);
    	var message =  JPushIM.buildMessageContent(juid, sid, "single", "text", toUserName, curChatUserId,
					uid, user_name, Date.parse(new Date())/1000, msg_body);	
    	JPushIM.chatEvent(message);
    } else if(isSingleOrGroup=='group'){
    	var toGroupName = $('#'+curChatGroupId).attr('displayname');
    	addToConversionList(curChatGroupId);  //   添加该会话到会话列表
    	updateConversionRectMsg(curChatGroupId, content);
    	var message = JPushIM.buildMessageContent(juid, sid, "group", "text", curChatGroupId, toGroupName,
				uid, curChatGroupId, Date.parse(new Date())/1000, msg_body);
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
	
	$('#conversionlistUL').prepend(node);
	$('#'+newId).append(recr_msg_elem);
}

//  更新会话列表中的最近消息状态
var updateConversionRectMsg = function(id, msg){
	$('#'+id+'-rect-msg').html(msg);
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
	if(contactLi == null){
		return;
	}
	if(conversionLi!=null){
		conversionLi.style.backgroundColor = "#B0E0E6";
	}
	contactLi.style.backgroundColor = "#B0E0E6";
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
	if(contactLi==null){
		return;
	}
	if(conversionLi){
		conversionLi.style.backgroundColor = "#B0E0E6";
	}
	contactLi.style.backgroundColor = "#B0E0E6";
	var group_name = $('#'+chatGroupId).attr("displayName");
	var dispalyTitle = "正在 " + group_name + " 群里聊天中";
	document.getElementById(talkToDivId).children[0].innerHTML = dispalyTitle;
};
	
//对上一个联系人的聊天窗口做隐藏处理
var hiddenContactChatDiv = function(chatUserId) {
	var contactLi = document.getElementById(chatUserId);
	var conversionLi = document.getElementById('conversion-'+chatUserId);
	if (contactLi) {
		contactLi.style.backgroundColor = "";
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
	$('#roomInfo').css({
		"visibility": "hidden"
	});
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
	$('#roomInfo').css({
		"visibility": "visible"
	});
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
		var header = $(headstr.join(''));

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
			var content = jQuery.parseJSON(message).content;
			var eletext = "<p3>" + content + "</p3>";
			ele = $(eletext);
			ele[0].setAttribute("class", "chat-content-p3");
			ele[0].setAttribute("className", "chat-content-p3");
			ele[0].style.backgroundColor = "#9EB867";
			updateConversionRectMsg(contactDivId, content);  //  更新会话列表中最新的消息
		} else if('image'==contentType){
			var messageObject = jQuery.parseJSON(message);
			var mediaId = messageObject.media_id;
			//var width = messageObject.width;
			//var height = messageObject.height;
			var path = JPushIM.QiNiuMediaUrl + mediaId + '?imageView2/2/h/100';
			message = "<img onclick='zoomOut(this)' src="+path+" width='100px;' height='70px;' style='cursor:pointer'></img>";
			var eletext = "<p3 >" + message + "</p3>";
			ele = $(eletext);
			ele[0].setAttribute("class", "chat-content-pic");
			ele[0].setAttribute("className", "chat-content-pic");
			ele[0].style.backgroundColor = "#9EB867";
			updateConversionRectMsg(contactDivId, "图片文件");  //  更新会话列表中最新的消息
		} else if('voice'==contentType){
			var messageObject = jQuery.parseJSON(message);
			var mediaId = messageObject.media_id;
			var duration = messageObject.duration;
			var mediaIds = mediaId.split('/');
			var path = "";
			if('qiniu'==mediaIds[1]){
				path = JPushIM.QiNiuMediaUrl + mediaId +'.mp3';
			} else if('upyun'==mediaIds[1]){
				path = JPushIM.UpYunVoiceMediaUrl + mediaId;
			}
			message = "<audio src=" + path +" controls='controls' width='40px' height='20px'>"+
							+	"Your browser does not support the audio element."+
							+ "</audio>";
			var eletext = "<p3 >" + message + "</p3>";
			ele = $(eletext);
			ele[0].setAttribute("class", "chat-content-voice");
			ele[0].setAttribute("className", "chat-content-voice");
			ele[0].style.backgroundColor = "#9EB867";
			updateConversionRectMsg(contactDivId, "语音文件");  //  更新会话列表中最新的消息
		}
		
		for ( var j = 0; j < ele.length; j++) {
			lineDiv.appendChild(ele[j]);
		}
					
		showUnreadMsgMark(name);
			 
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
			contactLi = getContactLi('conversion-'+contact);
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
			var content = jQuery.parseJSON(message).content;
			var eletext = "<p3>" + content + "</p3>";
			ele = $(eletext);
			ele[0].setAttribute("class", "chat-content-p3");
			ele[0].setAttribute("className", "chat-content-p3");
			ele[0].style.backgroundColor = "#9EB867";
			updateConversionRectMsg(contactDivId, content);  //  更新会话列表中最新的消息
		} else if('image'==contentType){
			var mediaId = jQuery.parseJSON(message).media_id;
			var path = JPushIM.QiNiuMediaUrl + mediaId + '?imageView2/2/h/100';
			message = "<img onclick='zoomOut(this)' src="+path+" width='100px;' height='70px;' style='cursor:pointer'></img>";
			var eletext = "<p3 >" + message + "</p3>";
			ele = $(eletext);
			ele[0].setAttribute("class", "chat-content-pic");
			ele[0].setAttribute("className", "chat-content-pic");
			ele[0].style.backgroundColor = "#9EB867";
			updateConversionRectMsg(contactDivId, "图片文件");  //  更新会话列表中最新的消息
		} else if('voice'==contentType){
			var messageObject = jQuery.parseJSON(message);
			var mediaId = messageObject.media_id;
			var duration = messageObject.duration;
			var mediaIds = mediaId.split('/');
			var path = "";
			if('qiniu'==mediaIds[1]){
				path = JPushIM.QiNiuMediaUrl + mediaId + '.mp3';
			} else if('upyun'==mediaIds[1]){
				path = JPushIM.UpYunVoiceMediaUrl + mediaId;
			}
			message = "<audio src=" + path +" controls='controls' width='40px' height='20px'>"+
							+	"Your browser does not support the audio element."+
							+ "</audio>";
			var eletext = "<p3 >" + message + "</p3>";
			ele = $(eletext);
			ele[0].setAttribute("class", "chat-content-voice");
			ele[0].setAttribute("className", "chat-content-voice");
			ele[0].style.backgroundColor = "#9EB867";
			updateConversionRectMsg(contactDivId, "语音文件");  //  更新会话列表中最新的消息
		}
		
		for ( var j = 0; j < ele.length; j++) {
			lineDiv.appendChild(ele[j]);
		}
					
		//if (curChatGroupId.indexOf(contact) < 0) {
			showUnreadMsgMark(contact);
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
};

//  emoji 选择框
var showEmotionDialog = function() {
	$('#wl_faces_box').css({
		"display" : "block"
	});
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
};
	
//  选择图片
var picfile;
$('#file').on('change', function(){
	picfile = this;
});

//  点击浏览原图
var zoomOut = function(obj){
	var src = $(obj).attr('src');
	var origin_src = src.split('?')[0];
	$('#zoomOutPic').attr('src', origin_src);
	$('#zoomOutPicView').modal('show');
};

//  查看群成员,显示群成员面板
var showGroupMembers = function(){
	//  拉取群成员信息
	JPushIM.getGroupMemberListEvent(curChatGroupId);
	var group_name = $('#'+curChatGroupId).attr('displayname');
	$('#group_info_groupname').val(group_name);
	$('#chat01.chat01').slideUp();
	$('.chat02').slideUp();
	$('#groupInfo').slideDown();
};
 
//  隐藏群成员信息，显示之前的聊天面板
var backToChat = function(){
	updateGroupName();
	$('#groupInfo').slideUp();
	$('#chat01.chat01').slideDown();
	$('.chat02').slideDown();
};

//  更新群聊名称
var updateGroupName = function(){
	var group_name = $('#group_info_groupname').val();
	var options = {
			sid : sid,
			juid : juid,
			uid : uid,
			user_name : user_name,
			gid : curChatGroupId,
			group_name : group_name
	};
	JPushIM.updateGroupNameEvent(options);
};

//  添加好友
var addNewFriends = function(){
	$('#addNewFriendModal').modal('show');
};

//  发送添加好友请求
var sendAddFriendCmd = function(){
	var friend_name = $('#friend_username').val();
	JPushIM.addFriendCmd({'from':curUserId, 'to':friend_name});
};

//  发送添加群成员请求
var sendAddGroupMemberCmd = function(){
	var username_list = $('#add_group_member_username').val();
	var options = {
		sid : sid,
		juid : juid,
		uid : uid,
		gid : curChatGroupId,
		member_count : 1,
		username : username_list
	};
	JPushIM.addGroupMemberEvent(options);
};

//  用户退出
var logout = function(){
	var options = {
			sid : sid,
			juid : juid,
			uid : uid,
			user_name : user_name
	};
	JPushIM.logoutEvent(options);
};

//  发起聊天
var startNewChat = function(){
	$('#startNewChatModal').modal('show');
};

// 发送发起聊天请求
var sendStartNewChatCmd = function(){
	
};

//  添加群组新成员
var addNewMember = function(){
	$('#addGroupMemberModal').modal('show');
}

//  显示群成员信息
var showMemberInfo = function(dom){
	var uid = $(dom).attr('id');
	var username = $('li#'+uid+' p.profileName').html();
	$('#groupMemberDetailId').val(uid);
	$('p.groupMemberDetailName').html(username);
	$('#showGroupMemberDetailInfoModal').modal('show');
}

// 新建与群成员的聊天会话  
var startNewChatWithGroupMember = function(){
	var toUid = $('#groupMemberDetailId').val();
	var toUserName = $('p.groupMemberDetailName').html();
	hideGroupInfoPanel();
	hiddenGroupChatDiv(curChatGroupId);
	showContactChatDiv(toUid);
};

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
	intervalId = setInterval("flashTitle()",2000);
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

