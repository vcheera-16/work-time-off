console.log('main.js loaded');

// helper to read cookie value by name
function readCookie(name) {
  const m = document.cookie.match('(^|;)\\s*' + name + '\\s*=\\s*([^;]+)');
  return m ? m.pop() : null;
}

// ensure showError exists so AJAX failures don't crash the script
function showError(msg) {
  try {
    var el = document.getElementById('errorArea');
    if (el) el.textContent = msg;
    else console.error(msg);
  } catch(e) { console.error(msg); }
  try { alert(msg); } catch(e){}
}

// Global jQuery AJAX setup: always include CSRF header for state-changing requests
if (window.jQuery) {
  $.ajaxSetup({
    beforeSend: function(xhr, settings) {
      if (!/^(GET|HEAD|OPTIONS|TRACE)$/.test(settings.type)) {
        var t = readCookie('XSRF-TOKEN');
        if (t) xhr.setRequestHeader('X-XSRF-TOKEN', t);
      }
    },
    xhrFields: { withCredentials: true }
  });
}

$(function(){
   var currentUser = null;

   // Login form
   $('#loginForm').on('submit', function(e){
     console.log('loginForm submit handler invoked');
     e.preventDefault();
     var email = $(this).find('[name=email]').val();
     var data = { email: email, password: $(this).find('[name=password]').val() };
     console.log('Attempting login for', email);

     var ready = (window.csrfReady || Promise.resolve());
     ready.then(function(){
       var tokenVal = readCookie('XSRF-TOKEN');
       console.log('login: sending XSRF token', tokenVal);
       $.ajax({
         url: '/api/auth/login',
         method: 'POST',
         contentType: 'application/json',
         beforeSend: function(xhr) {
           if (tokenVal) xhr.setRequestHeader('X-XSRF-TOKEN', tokenVal);
         },
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
     }).catch(function(){
       var tokenVal = readCookie('XSRF-TOKEN');
       console.log('login (fallback): sending XSRF token', tokenVal);
       $.ajax({
         url: '/api/auth/login',
         method: 'POST',
         contentType: 'application/json',
         beforeSend: function(xhr) {
           if (tokenVal) xhr.setRequestHeader('X-XSRF-TOKEN', tokenVal);
         },
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
   });

   // Apply timeoff
   $('#applyForm').on('submit', function(e){
     e.preventDefault();
     var payload = {
       type: $(this).find('[name=type]').val(),
       startDate: $(this).find('[name=startDate]').val(),
       endDate: $(this).find('[name=endDate]').val(),
       partialDay: $(this).find('[name=partialDay]').val()
     };
     console.log('Applying timeoff', payload);
     $.ajax({ url: '/api/timeoff', method: 'POST', contentType: 'application/json', data: JSON.stringify(payload) })
       .done(function(){
         alert('Request submitted');
         $('#applyForm')[0].reset();
         loadMyRequests();
       }).fail(function(xhr){
         showError('Failed to submit request');
       });
   });

   // Logout
   $('#logoutBtn').on('click', function(){
     $.ajax({ url: '/api/auth/logout', method: 'POST' }).always(function(){
       // show login panel
       window.currentUser = null;
       $('#dashboard').hide();
       $('#loginPanel').show();
     });
   });

   // Team approve/deny handlers (delegated)
   $('#teamTable tbody').on('click', 'button.approve', function(){
     var id = $(this).data('id');
     if(!confirm('Approve request #' + id + '?')) return;
     $.ajax({ url: '/api/timeoff/' + id + '/review', method: 'POST', contentType: 'application/json', data: JSON.stringify({ action: 'APPROVE' }) })
       .done(function(){ alert('Approved'); loadTeamRequests(); })
       .fail(function(){ showError('Failed to approve'); });
   });
   $('#teamTable tbody').on('click', 'button.deny', function(){
     var id = $(this).data('id');
     if(!confirm('Deny request #' + id + '?')) return;
     $.ajax({ url: '/api/timeoff/' + id + '/review', method: 'POST', contentType: 'application/json', data: JSON.stringify({ action: 'DENY' }) })
       .done(function(){ alert('Denied'); loadTeamRequests(); })
       .fail(function(){ showError('Failed to deny'); });
   });

   // helper: load personal requests
   function loadMyRequests(){
     $.getJSON('/api/timeoff').done(function(data){
       var rows = data.map(function(r){
         return [r.id, r.type, r.startDate, r.endDate, r.partialDay || '', r.status || 'PENDING'];
       });
       if ($.fn.dataTable.isDataTable('#myTable')) { $('#myTable').DataTable().clear().rows.add(rows).draw(); }
       else { $('#myTable').DataTable({ data: rows, columns: [ { title:'ID' },{ title:'Type' },{ title:'Start' },{ title:'End' },{ title:'Partial' },{ title:'Status' } ] }); }
     }).fail(function(){ console.warn('Failed to load personal requests'); });
   }

   // helper: load team requests (manager)
   function loadTeamRequests(){
     $.getJSON('/api/timeoff/team').done(function(data){
       var rows = data.map(function(r){
         var actions = '<button class="approve" data-id="'+r.id+'">Approve</button> <button class="deny" data-id="'+r.id+'">Deny</button>';
         return [r.id, (r.user && (r.user.fullName || r.user.email))||r.userEmail||'', r.type, r.startDate, r.endDate, r.status||'PENDING', actions];
       });
       if ($.fn.dataTable.isDataTable('#teamTable')) { $('#teamTable').DataTable().clear().rows.add(rows).draw(); }
       else { $('#teamTable').DataTable({ data: rows, columns: [ { title:'ID' },{ title:'User' },{ title:'Type' },{ title:'Start' },{ title:'End' },{ title:'Status' },{ title:'Actions', orderable:false } ] }); }
     }).fail(function(){ console.warn('Failed to load team requests'); });
   }

   // Expose load functions globally for showDashboard to call
   window._loadMyRequests = loadMyRequests;
   window._loadTeamRequests = loadTeamRequests;

});
