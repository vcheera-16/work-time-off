$(function(){
   var currentUser = null;
@@
   $('#loginForm').on('submit', function(e){
     e.preventDefault();
     var data = { email: $(this).find('[name=email]').val(), password: $(this).find('[name=password]').val() };
     $.ajax({
       url: '/api/auth/login',
       method: 'POST',
       contentType: 'application/json',
       data: JSON.stringify(data)
     }).done(function(resp){
       showDashboard(resp);
     }).fail(function(xhr){
       if(xhr.status === 401) showError('Invalid credentials');
       else if(xhr.status === 429) showError('Too many attempts. Try later.');
       else showError('Login failed');
     });
   });
@@
   $('#requestForm').on('submit', function(e){
     e.preventDefault();
     var payload = {
       type: $(this).find('[name=type]').val(),
       startDate: $(this).find('[name=startDate]').val(),
       endDate: $(this).find('[name=endDate]').val(),
       partialDay: $(this).find('[name=partialDay]').val()
     };
     $.ajax({
       url: '/api/timeoff',
       method: 'POST',
       contentType: 'application/json',
       data: JSON.stringify(payload)
     }).done(function(){
       alert('Request submitted');
       $('#requestForm')[0].reset();
     }).fail(function(xhr){
       var msg = 'Failed to submit request';
       try { msg = JSON.parse(xhr.responseText).error || msg; } catch(e){}
       showError(msg);
     });
   });
@@
   $('#teamTable tbody').on('click', 'button.approve', function(){
     var id = $(this).data('id');
     if(!confirm('Approve request #' + id + '?')) return;
     $.ajax({ url: '/api/timeoff/' + id + '/review', method: 'POST', contentType: 'application/json', data: JSON.stringify({ action: 'APPROVE' }) }).done(function(){ alert('Approved'); $('#showTeam').click(); }).fail(function(){ showError('Failed to approve'); });
   });
   $('#teamTable tbody').on('click', 'button.deny', function(){
     var id = $(this).data('id');
     if(!confirm('Deny request #' + id + '?')) return;
     $.ajax({ url: '/api/timeoff/' + id + '/review', method: 'POST', contentType: 'application/json', data: JSON.stringify({ action: 'DENY' }) }).done(function(){ alert('Denied'); $('#showTeam').click(); }).fail(function(){ showError('Failed to deny'); });
   });
@@
   $.get('/api/auth/me').done(function(user){ showDashboard(user); }).fail(function(){ /* not logged in */ });
 });
