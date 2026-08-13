console.log('main.js loaded');

$(function(){
   var currentUser = null;

   // Login form
   $('#loginForm').on('submit', function(e){
     console.log('loginForm submit handler invoked');
     e.preventDefault();
     var email = $(this).find('[name=email]').val();
     var data = { email: email, password: $(this).find('[name=password]').val() };
     console.log('Attempting login for', email);
     $.ajax({
       url: '/api/auth/login',
       method: 'POST',
       contentType: 'application/json',
       data: JSON.stringify(data)
     }).done(function(resp){
       console.log('login success', resp);
       showDashboard(resp);
     }).fail(function(xhr){
       console.log('login failed', xhr.status, xhr.responseText);
       if(xhr.status === 401) showError('Invalid credentials');
       else if(xhr.status === 429) showError('Too many attempts. Try later.');
       else showError('Login failed');
     });
   });

   // Timeoff request form
   $('#requestForm').on('submit', function(e){
     console.log('requestForm submit handler invoked');
     e.preventDefault();
     var payload = {
       type: $(this).find('[name=type]').val(),
       startDate: $(this).find('[name=startDate]').val(),
       endDate: $(this).find('[name=endDate]').val(),
       partialDay: $(this).find('[name=partialDay]').val()
     };
     console.log('Submitting timeoff request', payload);
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

   // Team table approve/deny (delegated)
   $('#teamTable tbody').on('click', 'button.approve', function(){
     console.log('approve clicked');
     var id = $(this).data('id');
     if(!confirm('Approve request #' + id + '?')) return;
     $.ajax({ url: '/api/timeoff/' + id + '/review', method: 'POST', contentType: 'application/json', data: JSON.stringify({ action: 'APPROVE' }) }).done(function(){ alert('Approved'); $('#showTeam').click(); }).fail(function(){ showError('Failed to approve'); });
   });
   $('#teamTable tbody').on('click', 'button.deny', function(){
     console.log('deny clicked');
     var id = $(this).data('id');
     if(!confirm('Deny request #' + id + '?')) return;
     $.ajax({ url: '/api/timeoff/' + id + '/review', method: 'POST', contentType: 'application/json', data: JSON.stringify({ action: 'DENY' }) }).done(function(){ alert('Denied'); $('#showTeam').click(); }).fail(function(){ showError('Failed to deny'); });
   });

   // On load, check if logged in
   $.get('/api/auth/me').done(function(user){
     console.log('/api/auth/me returned', user);
     showDashboard(user);
   }).fail(function(){
     console.log('/api/auth/me not logged in');
     /* not logged in */
   });

});
