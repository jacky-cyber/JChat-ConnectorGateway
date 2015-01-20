<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<html>
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">	
	<!-- 新 Bootstrap 核心 CSS 文件 -->
	<link rel="stylesheet" href="./res/css/bootstrap.min.css">
	<!-- 可选的Bootstrap主题文件（一般不用引入） -->
	<link rel="stylesheet" href="./res/css/bootstrap-theme.min.css">
	<style type="text/css">
		body {
  padding-top: 40px;
  padding-bottom: 40px;
  background-color: #eee;
}

.form-signin {
  max-width: 330px;
  padding: 15px;
  margin: 0 auto;
}
.form-signin .form-signin-heading,
.form-signin .checkbox {
  margin-bottom: 10px;
}
.form-signin .checkbox {
  font-weight: normal;
}
.form-signin .form-control {
  position: relative;
  height: auto;
  -webkit-box-sizing: border-box;
     -moz-box-sizing: border-box;
          box-sizing: border-box;
  padding: 10px;
  font-size: 16px;
}
.form-signin .form-control:focus {
  z-index: 2;
}
.form-signin input[type="email"] {
  margin-bottom: -1px;
  border-bottom-right-radius: 0;
  border-bottom-left-radius: 0;
}
.form-signin input[type="password"] {
  margin-bottom: 10px;
  border-top-left-radius: 0;
  border-top-right-radius: 0;
}
	</style>
   <title>Web IM Login</title>
  </head>
  <body>
    
     <div class="container">
      <form class="form-signin" role="form" action="uc/login">
        <h3 class="form-signin-heading">JPush IM</h3>
        <input type="text" name="user_name" id="user_name" class="form-control" placeholder="用户名" required autofocus>
        <button class="btn btn-lg btn-primary btn-block" type="submit">进入聊天</button>
      </form>
    </div> 
  </body>
</html>