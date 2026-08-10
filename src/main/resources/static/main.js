$(function(){
  function showDashboard(user){
    $('#auth').hide();
    $('#dashboard').show();
    if(user && user.email){
      $('#dashboard h2').text('Dashboard — ' + user.email);
    }
  }

  function showError(msg){
    alert(msg);
  }

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
      else showError('Login failed');
    });
  });

  // allow checking current session
  $('#showRequests').on('click', function(e){
    e.preventDefault();
    $('#requestsTableContainer').toggle();
    // TODO: load requests from /api/timeoff?mine=true
    var table = $('#requestsTable').DataTable();
    table.clear().rows.add([
      {id:1,type:'VACATION',start_date:'2026-08-15',end_date:'2026-08-20',status:'APPROVED',actions:''}
    ]).draw();
  });

  // try to fetch current user (if session exists)
  $.get('/api/auth/me').done(function(user){ showDashboard(user); }).fail(function(){ /* not logged in */ });
});
