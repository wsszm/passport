(function(){var a={b64_423:function(e){var f=new Array("A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z","a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z","0","1","2","3","4","5","6","7","8","9","-","_");var d=new String();for(var g=0;g<e.length;g++){for(var c=0;c<64;c++){if(e.charAt(g)==f[c]){var h=c.toString(2);d+=("000000"+h).substr(h.length);break}}if(c==64){if(g==2){return d.substr(0,8)}else{return d.substr(0,16)}}}return d},b2i:function(d){var c=0;var f=128;for(var e=0;e<8;e++,f=f/2){if(d.charAt(e)=="1"){c+=f}}return String.fromCharCode(c)},b64_decodex:function(d){var f=new Array();var e;var c="";for(e=0;e<d.length;e+=4){c+=a.b64_423(d.substr(e,4))}for(e=0;e<c.length;e+=8){f+=a.b2i(c.substr(e,8))}return f},utf8to16:function(h){var c,k,l,j,i,d,e,f,g;c=[];j=h.length;k=l=0;while(k<j){i=h.charCodeAt(k++);switch(i>>4){case 0:case 1:case 2:case 3:case 4:case 5:case 6:case 7:c[l++]=h.charAt(k-1);break;case 12:case 13:d=h.charCodeAt(k++);c[l++]=String.fromCharCode(((i&31)<<6)|(d&63));break;case 14:d=h.charCodeAt(k++);e=h.charCodeAt(k++);c[l++]=String.fromCharCode(((i&15)<<12)|((d&63)<<6)|(e&63));break;case 15:switch(i&15){case 0:case 1:case 2:case 3:case 4:case 5:case 6:case 7:d=h.charCodeAt(k++);e=h.charCodeAt(k++);f=h.charCodeAt(k++);g=((i&7)<<18)|((d&63)<<12)|((e&63)<<6)|(f&63)-65536;if(0<=g&&g<=1048575){c[l]=String.fromCharCode(((g>>>10)&1023)|55296,(g&1023)|56320)}else{c[l]="?"}break;case 8:case 9:case 10:case 11:k+=4;c[l]="?";break;case 12:case 13:k+=5;c[l]="?";break}}l++}return c.join("")},tmpl:function(e,d){var c=new Function("obj","var p=[],print=function(){p.push.apply(p,arguments);};with(obj){p.push('"+e.replace(/[\r\t\n]/g," ").split("<%").join("\t").replace(/((^|%>)[^\t]*)'/g,"$1\r").replace(/\t=(.*?)%>/g,"',$1,'").split("\t").join("');").split("%>").join("p.push('").split("\r").join("\\'")+"');}return p.join('');");return d?c(d):c},addIframe:function(e,d,f){var c=document.createElement("iframe");c.style.height="1px";c.style.width="1px";c.style.visibility="hidden";c.src=d;if(c.attachEvent){c.attachEvent("onload",function(){f&&f()})}else{c.onload=function(){f&&f()}}e.appendChild(c)},uuid:function(){function c(){return Math.floor((1+Math.random())*65536).toString(16).substring(1)}return c()+c()+c()+c()+c()+c()+c()+c()}};var b=window.PassportSC||{};b._token=a.uuid();b._passhtml='<form method="post" action="https://account.sogou.com/web/login" target="_PassportIframe"><input type="hidden" name="username" value="<%=username%>"><input type="hidden" name="password" value="<%=password%>"><input type="hidden" name="captcha" value="<%=vcode%>"><input type="hidden" name="autoLogin" value="<%=isAutoLogin%>"><input type="hidden" name="client_id" value="<%=appid%>"><input type="hidden" name="xd" value="<%=redirectUrl%>"><input type="hidden" name="token" value="<%=token%>"></form><iframe id="_PassportIframe" name="_PassportIframe" src="about:blank" style="width：1px;height:1px;position:absolute;left:-1000px;"></iframe>';b._logincb=function(c){if(!+c.status){b.onsuccess&&b.onsuccess(c)}else{if(+c.status==20231){location.href="https://account.sogou.com/web/remindActivate?email="+encodeURIComponent(b._currentUname)+"&client_id="+b.appid+"&ru="+encodeURIComponent(location.href)}else{b.onfailure&&b.onfailure(c)}}};b.getToken=function(){return b._token};b._checkCommon=function(c){if(!b.redirectUrl&&!c){window.console&&console.log("Must specify redirect url.Exit!");return}if(!b.appid){window.console&&console.log("Must specify appid.Exit!");return}return true};b.loginHandle=function(h,e,i,g,f,d,c){if(arguments.length<7){c=d;d=f;f=g;g=i;i=""}if(!b._checkCommon()){return}if(!f){return}b._currentUname=h;b.onsuccess=c,b.onfailure=d;f.innerHTML=a.tmpl(b._passhtml,{username:h,password:e,vcode:i,isAutoLogin:g,appid:b.appid,redirectUrl:b.redirectUrl,token:b._token});f.getElementsByTagName("form")[0].submit()};b.logoutHandle=function(f,d,c){if(!f){return}if(!b._checkCommon(true)){return}var e="https://account.sogou.com/web/logout_js?client_id="+b.appid;a.addIframe(f,e,function(){c&&c()})};b._parsePassportCookie=function(m){var i;var d;var c;this.cookie=new Object;i=0;d=m.indexOf(":",i);while(d!=-1){var e;var f;var j;e=m.substring(i,d);var g=m.indexOf(":",d+1);if(g==-1){break}f=parseInt(m.substring(d+1,g));j=m.substr(g+1,f);if(m.charAt(g+1+f)!="|"){break}this.cookie[e]=j;i=g+2+f;d=m.indexOf(":",i)}var h=this._parserRelation();if(h!=null&&h.length>0){this.cookie[e]=h}try{this.cookie.service=new Object;var k=this.cookie.service;k.mail=0;k.alumni=0;k.chinaren=0;k.blog=0;k.pp=0;k.club=0;k.crclub=0;k.group=0;k.say=0;k.music=0;k.focus=0;k["17173"]=0;k.vip=0;k.rpggame=0;k.pinyin=0;k.relaxgame=0;var l=this.cookie.serviceuse;if(l.charAt(0)==1){k.mail="sohu"}else{if(l.charAt(2)==1){k.mail="sogou"}else{if(this.cookie.userid.indexOf("@chinaren.com")>0){k.mail="chinaren"}}}if(l.charAt(1)==1){k.alumni=1}if(l.charAt(3)==1){k.blog=1}if(l.charAt(4)==1){k.pp=1}if(l.charAt(5)==1){k.club=1}if(l.charAt(7)==1){k.crclub=1}if(l.charAt(8)==1){k.group=1}if(l.charAt(10)==1){k.music=1}if(l.charAt(11)==1||this.cookie.userid.lastIndexOf("@focus.cn")>0){k.focus=1}if(l.charAt(12)==1||this.cookie.userid.indexOf("@17173.com")>0){k["17173"]=1}if(l.charAt(13)==1){k.vip=1}if(l.charAt(14)==1){k.rpggame=1}if(l.charAt(15)==1){k.pinyin=1}if(l.charAt(16)==1){k.relaxgame=1}}catch(n){}};b._parseCookie=function(){var d=document.cookie.split("; ");var g;for(var e=0,c=d.length;e<c;e++){if(d[e].indexOf("ppinf=")==0){g=d[e].substr(6);break}if(d[e].indexOf("ppinfo=")==0){g=d[e].substr(7);break}if(d[e].indexOf("passport=")==0){g=d[e].substr(9);break}}if(!g){this.cookie=false;return}try{g=unescape(g).split("|");if(g[0]=="1"||g[0]=="2"){g=a.utf8to16(a.b64_decodex(g[3]));this._parsePassportCookie(g);return}}catch(f){}};b.cookieHandle=function(){this._parseCookie();if(this.cookie&&this.cookie.userid!=""){return this.cookie.userid}else{return""}};b._authConfig={size:{renren:[880,620],sina:[780,640],qq:[500,300]}};b.authHandle=function(j,i,e,d){if(!b._checkCommon()){return}if(!j){window.console&&console.log("Must specify provider.Exit!");return}i=i||"page";if(i=="page"&&(typeof e=="function"||typeof d=="function")){window.console&&console.log("When display is page, onfailure & onsuccess must be url.Exit!");return}var c=i=="popup"?b.redirectUrl:(d||location.href);var g="http://account.sogou.com/connect/login?client_id="+b.appid+"&provider="+j+"&ru="+encodeURIComponent(c);if(i=="popup"){var f=b._authConfig.size[j];var h=(window.screen.availWidth-f[0])/2;window.open(g,"OPEN_LOGIN","height="+f[1]+",width="+f[0]+",top=80,left="+h+",toolbar=no,menubar=no")}else{if(i=="page"){location.href=g}}};window.PassportSC=b;if(b.onApiLoaded&&typeof b.onApiLoaded=="function"){b.onApiLoaded()}})();