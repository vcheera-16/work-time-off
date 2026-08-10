$(function(){
  function showDashboard(){
    $('#auth').hide();
    $('#dashboard').show();
  }

  $('#loginForm').on('submit', function(e){
    e.preventDefault();
    var data = { email: $(this).find('[name=email]').val(), password: $(this).find('[name=password]').val() };
    // TODO: call backend /api/auth/login
    console.log('login', data);
    // For now simulate success
    showDashboard();
  });

  var table = $('#requestsTable').DataTable({ columns: [ {data:'id'}, {data:'type'}, {data:'start_date'}, {data:'end_date'}, {data:'status'}, {data:'actions'} ] });

  $('#showRequests').on('click', function(e){
    e.preventDefault();
    $('#requestsTableContainer').toggle();
    // TODO: load requests from /api/timeoff?mine=true
    table.clear().rows.add([
      {id:1,type:'VACATION',start_date:'2026-08-15',end_date:'2026-08-20',status:'APPROVED',actions:''}
    ]).draw();
  });
});
