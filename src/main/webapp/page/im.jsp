<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<html>
<head>
<title>Testing websockets</title>
<!-- 新 Bootstrap 核心 CSS 文件 -->
<link rel="stylesheet" href="../res/css/bootstrap.min.css">
<!-- 可选的Bootstrap主题文件（一般不用引入） -->
<link rel="stylesheet" href="../res/css/bootstrap-theme.min.css">
</head>
<body>

        <div class="container">
          	<div class="row">
  					 <div class="col-md-6">
  					 		<select multiple="multiple" id="messages" style="position:relative; width:220px; height:150px; vertical-align:top; border:1px solid #cccccc;"></select>
  					 </div>
  				</div>
  				<div class="row">
  					 <div class="col-md-6">
  					 		<textarea name="textarea" id="textarea" cols="10" rows="5"></textarea>
  					 </div>
  				</div>
  				<div class="row">
  					 <div class="col-md-4">
  					 		<input type="submit" value="发送" onclick="start()" />
  					 </div>
  				</div>  
          </div>
 <%--  <script type="text/javascript">
  	 var user_name = '<%=request.getParameter("user_name")%>';
    var webSocket = new WebSocket('ws://127.0.0.1:8080/webim/im?user_name='+user_name);
 
    webSocket.onerror = function(event) {
      onError(event)
    };
 
    webSocket.onopen = function(event) {
      onOpen(event)
    };
 
    webSocket.onmessage = function(event) {
      onMessage(event)
    };
 
    function onMessage(event) {
    	var option=document.createElement("option");
		option.value=event.data;
		option.innerHTML=event.data;
		document.getElementById('messages').appendChild(option);
      //document.getElementById('messages').append('<br/>' + event.data);
    }
 
    function onOpen(event) {
      /* document.getElementById('messages').innerHTML
        = 'Connection established'; */
    }
 
    function onError(event) {
      alert(event.data);
    }
 
    function start() {
    	var content = document.getElementById('textarea').value;
      webSocket.send(content);
      document.getElementById('textarea').value = '';
      return false;
    }
  </script> --%>
  <script type="text/javascript" src="//cdn.jsdelivr.net/sockjs/0.3.4/sockjs.min.js"></script>
  <script type="text/javascript">
  var user_name = '<%=request.getParameter("user_name")%>';
  //var url = 'http://127.0.0.1:8080/webim/im?user_name='+user_name;
  var url = 'http://127.0.0.1:8080/webim/im/';
  var sock = new SockJS(url);
  sock.onopen = function() {
	   sock.send("user_name:"+user_name);
      console.log('open');
   };
  sock.onmessage = function(event) {
	  	var option=document.createElement("option");
		option.value=event.data;
		option.innerHTML=event.data;
		document.getElementById('messages').appendChild(option);
   };
  sock.onclose = function() {
      console.log('close');
   };
  function start() {
   	var content = document.getElementById('textarea').value;
   	sock.send(content);
     	document.getElementById('textarea').value = '';
     	return false;
   };
  </script>
</body>
</html>
