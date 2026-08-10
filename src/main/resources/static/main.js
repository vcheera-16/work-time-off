$(function(){
  var currentUser = null;

  function showDashboard(user){
    $('#auth').hide();
    $('#dashboard').show();
    currentUser = user;
    if(user && user.email){
      $('#dashboard h2').text('Dashboard — ' + user.email);
    }
    if(user && (user.role === 'MANAGER' || user.role === 'ADMIN')){
      $('#showTeam').show();
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

  $('#showRequestForm').on('click', function(e){
    e.preventDefault();
    $('#requestFormContainer').toggle();
  });

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

  var table = $('#requestsTable').DataTable({ columns: [ {data:'id'}, {data:'type'}, {data:'startDate'}, {data:'endDate'}, {data:'status'}, {data:'actions'} ] });
  var teamTable = $('#teamTable').DataTable({ columns: [ {data:'id'}, {data:'userId'}, {data:'type'}, {data:'startDate'}, {data:'endDate'}, {data:'status'}, {data:'actions'} ] });

  $('#showRequests').on('click', function(e){
    e.preventDefault();
    $('#teamTableContainer').hide();
    $('#requestsTableContainer').toggle();
    if($('#requestsTableContainer').is(':visible')){
      $.get('/api/timeoff?mine=true').done(function(data){
        table.clear();
        data.forEach(function(r){ r.actions = r.status === 'PENDING' ? '<button class="cancel" data-id="'+r.id+'">Cancel</button>' : '' });
        table.rows.add(data).draw();
      }).fail(function(){ showError('Failed to load requests'); });
    }
  });

  $('#requestsTable tbody').on('click', 'button.cancel', function(){
    var id = $(this).data('id');
    if(!confirm('Cancel request #' + id + '?')) return;
    $.post('/api/timeoff/' + id + '/cancel').done(function(){
      alert('Cancelled');
      $('#showRequests').click();
    }).fail(function(xhr){ showError('Failed to cancel'); });
  });

  $('#showTeam').on('click', function(e){
    e.preventDefault();
    $('#requestsTableContainer').hide();
    $('#teamTableContainer').toggle();
    if($('#teamTableContainer').is(':visible')){
      $.get('/api/timeoff/team').done(function(data){
        teamTable.clear();
        data.forEach(function(r){ r.actions = r.status === 'PENDING' ? '<button class="approve" data-id="'+r.id+'">Approve</button> <button class="deny" data-id="'+r.id+'">Deny</button>' : '' });
        teamTable.rows.add(data).draw();
      }).fail(function(){ showError('Failed to load team requests'); });
    }
  });

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

  // try to fetch current user (if session exists)
  $.get('/api/auth/me').done(function(user){ showDashboard(user); }).fail(function(){ /* not logged in */ });
});
