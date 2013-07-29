define("utils",[],function(){return{uuid:function(){function s4(){return Math.floor((1+Math.random())*65536).toString(16).substring(1)}return s4()+s4()+s4()+s4()+s4()+s4()+s4()+s4()},addZero:function(num,len){num=num.toString();while(num.length<len){num="0"+num}return num},parseResponse:function(data){if(typeof data=="string"){try{data=eval("("+data+")")}catch(e){data={status:-1,statusText:"服务器故障"}}}return data},addIframe:function(url,callback){var iframe=document.createElement("iframe");iframe.src=url;iframe.style.position="absolute";iframe.style.top="1px";iframe.style.left="1px";iframe.style.width="1px";iframe.style.height="1px";if(iframe.attachEvent){iframe.attachEvent("onload",function(){callback&&callback()})}else{iframe.onload=function(){callback&&callback()}}document.body.appendChild(iframe)},getScript:function(url,callback){var script=document.createElement("script");var head=document.head;script.async=true;script.src=url;script.onload=script.onreadystatechange=function(_,isAbort){if(isAbort||!script.readyState||/loaded|complete/.test(script.readyState)){script.onload=script.onreadystatechange=null;if(script.parentNode){script.parentNode.removeChild(script)}script=null;if(!isAbort){callback()}}};head.insertBefore(script,head.firstChild)},getUrlByMail:function(mail){mail=mail.split("@")[1];if(!mail){return false}var hash={"139.com":"mail.10086.cn","gmail.com":"mail.google.com","sina.com":"mail.sina.com.cn","yeah.net":"www.yeah.net","hotmail.com":"www.hotmail.com","live.com":"www.outlook.com","live.cn":"www.outlook.com","live.com.cn":"www.outlook.com","outlook.com":"www.outlook.com","yahoo.com.cn":"mail.cn.yahoo.com","yahoo.cn":"mail.cn.yahoo.com","ymail.com":"www.ymail.com","eyou.com":"www.eyou.com","188.com":"www.188.com","foxmail.com":"www.foxmail.com"};var url;if(mail in hash){url=hash[mail]}else{url="mail."+mail}return"http://"+url}}});define("common",["./utils"],function(a){return{addUrlCommon:function(b){if(b.ru){$(".main-content .nav li a,.banner li a").each(function(c,d){$(d).attr("href",$(d).attr("href")+"?ru="+encodeURIComponent(b.ru))})}if(b.client_id){$(".main-content .nav li a,.banner li a").each(function(c,d){$(d).attr("href",$(d).attr("href")+($(d).attr("href").indexOf("?")==-1?"?":"&")+"client_id="+b.client_id)})}},showBannerUnderLine:function(){$(".banner ul").show();var b=$(".banner ul li.current");if(b.length){$(".banner .underline").css("left",b.position().left).css("width",b.css("width"))}},parseHeader:function(b){$("#Header .username").html(b.username);if(b.username){$("#Header .info").show()}},bindJumpEmail:function(){$("#JumpToUrl").click(function(){if($("#JumpTarget")){window.open(a.getUrlByMail($("#JumpTarget").html()))}return false})}}});var __ssjs__=typeof exports=="undefined"?false:true;if(__ssjs__){var Ursa={varType:{},escapeType:{}}}(function(){if(!__ssjs__){if(typeof Ursa!="undefined"&&typeof Ursa.render!="undefined"){return}window.Ursa=window.Ursa||{varType:{},escapeType:{}}}var config={starter:"{",ender:"}",commentStarter:"#",commentEnder:"#",opStarter:"{",opEnder:"}",statementStarter:"%",statementEnder:"%"},starter=config.starter,ender=config.ender,commentStarter=config.commentStarter,commentEnder=config.commentEnder,opStarter=config.opStarter,opEnder=config.opEnder,statementStarter=config.statementStarter,statementEnder=config.statementEnder,endStartReg=new RegExp("["+opEnder+commentEnder+statementEnder+"]","g");function setConfig(conf){for(var i in conf){if(config[i]){config[i]=conf[i]}}starter=config.starter,ender=config.ender,commentStarter=config.commentStarter,commentEnder=config.commentEnder,opStarter=config.opStarter,opEnder=config.opEnder,statementStarter=config.statementStarter,statementEnder=config.statementEnder}function range(start,end,size){var res=[],size=size||1;if(start<=end){while(start<end){res.push(start);start+=size*1}}else{while(start>end){res.push(start);start=start-size}}return res}function each(rge,callback){if(rge instanceof Array){for(var i=0,len=rge.length;i<len;i++){callback&&callback(rge[i],i,i)}}else{if(rge instanceof Object){var index=0;for(var key in rge){if(typeof rge[key]!="function"){callback&&callback(rge[key],key,index);index++}}}}}function dumpError(code,tplString,pointer,matches){var msg;switch(code){case 1:msg="错误的使用了\\，行数:"+getLineNumber(tplString,pointer);break;case 2:msg='缺少结束符}"，行数:'+getLineNumber(tplString,pointer);break;case 3:msg='缺少"{","#"或者"%"，行数:'+getLineNumber(tplString,pointer);break;case 4:msg="未闭合的{，,行数:"+getLineNumber(tplString,pointer);break;case 5:msg="以下标签未闭合"+matches.join(",");break;case 6:msg="创建模板失败"+tplString;break;case 7:msg='缺少"'+matches.replace("end","")+",行数:"+getLineNumber(tplString,pointer);break;case 8:msg="缺少结束符}"+tplString;break;default:msg="出错了";break}throw new Error(msg)}var __undefinded;function cleanWhiteSpace(result){result=result.replace(/\t/g,"    ");result=result.replace(/\r\n/g,"\n");result=result.replace(/\r/g,"\n");result=result.replace(/^(\s*\S*(\s+\S+)*)\s*$/,"$1");return result}function _length(rge){if(!rge){return 0}if(rge instanceof Array){return rge.length}var length=0;each(rge,function(item,i,index){length=index+1});return length}function _jsIn(key,rge){if(!key||!rge){return false}if(rge instanceof Array){for(var i=0,len=rge.length;i<len;i++){if(key==rge[i]){return true}}}try{return rge.match(key)?true:false}catch(e){return false}}function _jsIs(vars,type,args3,args4){switch(type){case"odd":return vars%2==1;break;case"even":return vars%2==0;break;case"divisibleby":return vars%args3==0;break;case"defined":return typeof vars!="undefined";break;default:if(Ursa.varType&&Ursa.varType[type]){return Ursa.varType[type].apply(null,arguments)}else{return false}}}function _trim(str){return str?(str+"").replace(/(^\s*)|(\s*$)/g,""):""}function _default(vars){return vars}function _abs(vars){return Math.abs(vars)}function _format(vars){if(!vars){return""}var placeHolder=vars.split(/%s/g);var str="",arg=arguments;each(placeHolder,function(item,key,i){str+=item+(arg[i+1]?arg[i+1]:"")});return str}function _join(vars,div){if(!vars){return""}if(vars instanceof Array){return vars.join(typeof div!="undefined"?div:",")}return vars}function _replace(str,replacer){if(!str){return""}var str=str;each(replacer,function(value,key){str=str.replace(new RegExp(key,"g"),value)});return str}function _slice(arr,start,length){if(arr&&arr.slice){return arr.slice(start,start+length)}else{return arr}}function _sort(arr){if(arr&&arr.sort){arr.sort(function(a,b){return a-b})}return arr}function _escape(str,type){if(typeof str=="undefined"||str==null){return""}if(str&&(str.safe==1)){return str.str}var str=str.toString();if(type=="js"){return str.replace(/\'/g,"\\'").replace(/\"/g,'\\"')}if(type=="none"){return{str:str,safe:1}}if(Ursa.escapeType&&Ursa.escapeType[type]){return Ursa.escapeType[type](str)}return str.replace(/<|>/g,function(m){if(m=="<"){return"&lt;"}return"&gt;"})}function _raw(str){return{safe:1,str:str}}function _truncate(str,len,killwords,end){if(typeof str=="undefined"){return""}var str=new String(str);var killwords=killwords||false;var end=typeof end=="undefined"?"...":"";if(killwords){return(typeof len=="undefined"?str.substr(0,str.length):str.substr(0,len)+(str.length<=len?"":end))}return end}function _substring(str,start,end){if(typeof str=="undefined"){return""}var str=new String(str);var end=typeof end!="undefined"?end:str.length;return str.substring(start,end)}function _upper(str){if(typeof str=="undefined"){return""}return new String(str).toUpperCase()}function _lower(str){if(typeof str=="undefined"){return""}return new String(str).toLowerCase()}Ursa._tpl={};Ursa.render=function(tplName,data,tplString){if(!Ursa._tpl[tplName]){Ursa.compile(tplString,tplName)}return Ursa._tpl[tplName](data)};Ursa.compile=function(tplString,tplName){var str=SyntaxGetter(tplString);try{eval('Ursa._tpl["'+tplName+'"] = '+str)}catch(e){dumpError(6,e)}return Ursa._tpl[tplName]};var tags="^(for|endfor|if|elif|else|endif|set)";var tagsReplacer={"for":{validate:/for[\s]+[^\s]+\sin[\s]+[\S]+/g,pfixFunc:function(obj){var statement=obj.statement,args=statement.split(/[\s]+in[\s]+/g)[0],_args,_value=_args,_key=args,context=statement.replace(new RegExp("^"+args+"[\\s]+in[\\s]+","g"),"");if(args.indexOf(",")!=-1){args=args.split(",");if(args.length>2){dumpError('多余的","在'+args.join(","),"tpl")}_key=args[0];_value=args[1];_args=args.reverse().join(",")}else{_key="_key";_value=args;_args=args+",_key"}return"(function() {var loop = {index:0,index0:-1,length: _length("+context+")}; if(loop.length > 0) {each("+context+", function("+_args+") {loop.index ++;loop.index0 ++;loop.key = "+_key+";loop.value = "+_value+";loop.first = loop.index0 == 0;loop.last = loop.index == loop.length;"}},endfor:{pfixFunc:function(obj,hasElse){return(hasElse?"":"})")+"}})();"}},"if":{validate:/if[\s]+[^\s]+/g,pfixFunc:function(obj){var statement=obj.statement;var tests=compileOperator(statement);return"if("+tests},sfix:") {"},elif:{validate:/elif[\s]+[^\s]+/g,pfixFunc:function(obj){var statement=obj.statement;var tests=compileOperator(statement);return"} else if("+tests},sfix:") {"},"else":{pfixFunc:function(obj,start){if(start=="for"){return"})} else {"}return"} else {"}},endif:{pfix:"}"},set:{validate:/set[\s]+[^\s]+/g,pfixFunc:function(obj){var statement=obj.statement;var tests=compileOperator(statement);return"var "+tests},sfix:";"}};var operator="\\/\\/|\\*\\*|\\||in|is";var operatorReplacer={"//":{pfix:"parseInt(",sfix:")"},"**":{pfixFunc:function(){return"Math.pow("},sfix:")"},"|":{sfix:")"},"in":{pfixFunc:function(vars){return"_jsIn(((typeof "+vars+' != "undefined") ? '+vars+": __undefinded)"},sfix:")"},is:{pfixFunc:function(vars){return"_jsIs(typeof "+vars+' != "undefined" ? '+vars+" : __undefinded"},sfix:")"},and:{pfixFunc:function(obj){var statement=obj.statement;return statement.replace(/[\s]*and[\s]*/g," && ")}},or:{pfixFunc:function(obj){var statement=obj.statement;return statement.replace(/[\s]*or[\s]*/g," || ")}},not:{pfixFunc:function(obj){var statement=obj.statement;return statement.replace(/[\s]*not[\s]*/g,"!")}}};function merge(obj,opstatement,start){return(obj.pfixFunc&&obj.pfixFunc(opstatement,start)||obj.pfix||"")+(opstatement.sfix||obj.sfix||"")}function funcVars(str){var str=str.replace(/\([\s]*\)/g,"").replace(/[\s\(]+/g,",").replace(/\)$/g,"");var dot=str.indexOf(",");if(dot==-1){str+='"'}else{str=str.substring(0,dot)+'"'+str.substring(dot)}return str}function redoGetStrings(str,bark){each(bark,function(value,key){str=str.replace(new RegExp(key,"g"),value)});return str}function compileOperator(opstatement){var reg=new RegExp("(^(not)|[\\s]+(and|or|not))[\\s]+","g"),matches;opstatement=opstatement.replace(/[^\s\(\)]+[\s]+is[\s]+not[\s]+[^\s\(\)]+(\([^\)]*\))?/g,function(m){var vars=m.split(/[\s]+is[\s]+not/);var str="!"+operatorReplacer.is["pfixFunc"](vars[0]);vars.splice(0,1);vars=funcVars(_trim(vars.join("")));return str+(vars?', "'+vars+"":"")+operatorReplacer.is["sfix"]});opstatement=opstatement.replace(/[^\s\(\)]+[\s]+is[\s]+[^\s\(\)]+(\([^\)]*\))?/g,function(m){var vars=m.split(/[\s]+is[\s]+/);var str=operatorReplacer.is["pfixFunc"](vars[0]);vars.splice(0,1);vars=funcVars(_trim(vars.join("is")));return str+(vars?', "'+vars+"":"")+operatorReplacer.is["sfix"]});var vars=opstatement.match(/[^\s]+[\s]+in[\s]+[^\s]+/g);if(vars){for(var i=0,len=vars.length;i<len;i++){var varName=vars[i].split(/[\s]+/g);var rge=varName[varName.length-1];varName=varName[0];opstatement=opstatement.replace(vars[i],operatorReplacer["in"].pfixFunc(varName)+","+rge+operatorReplacer["in"].sfix)}}opstatement=opstatement.replace(reg,function(m){var m=_trim(m);if(m=="not"){return"!"}if(m=="and"){return"&&"}if(m=="or"){return"||"}});return opstatement}function output(source){source=source.split("|");var str=compileOperator(source[0]);for(var i=1,len=source.length;i<len;i++){var func="_"+_trim(source[i]);var fs=func.split("(");var fname=_trim(fs[0]);fs.splice(0,1);fs=_trim(fs.join("("));if(fname=="_default"){str=fname+"( typeof "+str+' == "undefined" ? '+fs.replace(/\)$/g,"")+" : "+str+")"}else{str=fname+"("+str+((!fs||fs==")")?")":","+fs)}}return"__output.push(_escape("+str+"));"}function getLineNumber(tplString,pointer){return tplString?(tplString.substr(0,pointer+1).match(/\n/g)||[]).length+1:0}function setKeyV(obj,value){var k=Math.random()*100000>>0;while(!obj["__`begin`__"+k+"__`end`__"]){k++;obj["__`begin`__"+k+"__`end`__"]=value}return"__`begin`__"+k+"__`end`__"}Ursa.ioStart=function(){return"function (__context) {var __output = [];with(__context) {"};Ursa.ioEnd=function(){return'};return __output.join("");}'};Ursa.ioHTML=function(ins){return'__output.push("'+_escape(ins,"js")+'");'};Ursa.ioOutput=function(ins){return output(ins)};Ursa.ioOP=function(ins){return compileOperator(ins)+";"};Ursa.ioMerge=function(matches,sourceObj,flag){return merge(tagsReplacer[matches],sourceObj,flag)};Ursa.set=function(key,value){Ursa[key]=value};function SyntaxGetter(tplString){var pointer=-1,tplString=cleanWhiteSpace(tplString),character,stack="",statement="",endType="",tree=[],oldType,result=Ursa.ioStart(),tagStack=[],tagStackPointer=[],strDic={},type=false;while((character=tplString.charAt(++pointer))!=""){id=tagStackPointer.length;if(type==3){if(character==commentEnder){character=tplString.charAt(++pointer);if(character==ender){type=false}}continue}if(type%3==1&&(character=="'"||character=='"')){var start=tplString.charAt(pointer),tmpStr=start;while((character=tplString.charAt(++pointer))&&(character!=start)){if(character=="\\"){tmpStr+="\\";character=tplString.charAt(++pointer)}tmpStr+=character}tmpStr+=start;stack+=setKeyV(strDic,tmpStr)}else{if(character=="\\"){type=2;stack+=character+character}else{if(character==starter){character=tplString.charAt(++pointer);oldType=type;switch(character){case commentStarter:type=3;break;case opStarter:type=4;break;case statementStarter:type=1;break;default:stack+=starter;if(character.match(/[\'\"]/g)){pointer--}else{stack+=character}continue;break}if(oldType==2){result+=Ursa.ioHTML(stack);stack=""}else{if(character==ender){}}}else{if(endType=character.match(endStartReg)){endType=endType[0];if(type!=2){character=tplString.charAt(++pointer);if(character==ender){if(endType==opEnder){result+=Ursa.ioOutput(_trim(stack))}else{var start=tagStackPointer[tagStackPointer.length-1],matches,flag=start&&start.type,source=_trim(stack),id=1;if((matches=source.match(tags))){matches=matches[0];if(matches.indexOf("end")==0){id=tagStackPointer.length;flag=tagStack.splice(start.p,tagStack.length-start.p).length>1;tagStackPointer.splice(tagStackPointer.length-1,1)}else{if(matches!="set"){tagStack.push(matches);if(matches=="if"||matches=="for"){tagStackPointer.push({p:tagStack.length-1,type:matches})}id=tagStackPointer.length}}result+=Ursa.ioMerge(matches,{statement:source.replace(new RegExp("^"+matches+"[\\s]*","g"),"")},flag)}else{result+=Ursa.ioOP(source)}}type=false;stack="";continue}else{if(character.match(endStartReg)){pointer--;stack+=endType;continue}else{stack+=endType+character}}}else{stack+=endType}}else{if(!type){type=2}stack+=character}}}}}if(stack){if(type==2){result+=Ursa.ioHTML(stack);stack=null}else{dumpError(8,stack)}}result+=Ursa.ioEnd();if(tagStack.length){dumpError(5,tplString,pointer,tagStack)}return redoGetStrings(result.replace(/\n/g,""),strDic)}Ursa.parse=SyntaxGetter;Ursa.setConfig=setConfig})();if(__ssjs__){exports.Ursa=Ursa}else{if(window.define){define("Ursa",[],function(){return Ursa})}}define("tpl",["./Ursa"],function(a){a.setConfig({starter:"<",ender:">"});return{render:function(b,c){return a.render(+new Date(),c,b)}}});define("ucenter",["./common","./tpl"],function(a,c){var b={common:function(d){a.parseHeader(d)},disable:function(e){var d=$("#TargetDisable");var f=$("#Target").parent();f.html(c.render(d.html(),e));$($(".banner li")[1]).hide();$(".sidebar .ucenter-sidebar span.dynamic").hide();$(".sidebar .ucenter-sidebar .hr").hide()},index:function(g){var d=$("#Target");var h=d.parent();var e=new Date(+g.last_login_time);g.time={year:e.getFullYear(),month:e.getMonth()+1,day:e.getDate()};h.html(c.render(d.html(),g));h.find(".level-status b").css("width",g.sec_score+"%");if(g.actype=="phone"){var f=$('.ucenter-sidebar a[href="/web/security/mobile"]').parent();f.hide();$(f.parent().find(".hr")[1]).hide()}}};return{init:function(d){a.showBannerUnderLine();var f={};try{f=$.evalJSON(server_data).data}catch(g){window.console&&console.log(g)}f.actype=f.actype||"";b.common(f);if(f.disable){b.disable(f)}else{b[d]&&b[d](f)}$(".sidebar .ucenter-sidebar").show()}}});