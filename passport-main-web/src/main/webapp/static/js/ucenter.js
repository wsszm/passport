/*
 * ucenter module script
 * @author zhengxin
*/
 



define(['./common' , './tpl'] , function(common , ursa ){

    var pagefunc = {
        common: function(data){
            $('#Header .username').html(data.username);
        },
        index: function(data){
            var tpl = $('#Target');
            var wrapper = tpl.parent();
            var last_login_time = new Date(+data.last_login);
            data.time = {
                year: last_login_time.getFullYear(),
                month: last_login_time.getMonth()+1,
                day: last_login_time.getDate()
            };

            wrapper.html( ursa.render(tpl.html() , data));
            wrapper.find('.level-status b').css( 'width' , data.sec_score + '%' );
            
        }

    };


    return{
        init: function(type){
            common.showBannerUnderLine();

            var data ={};
            try{
                data = $.evalJSON(server_data).data;
            }catch(e){window['console'] && console.log(e);}
            
            pagefunc.common(data);

            pagefunc[type] && pagefunc[type](data);
        }
    };
});
