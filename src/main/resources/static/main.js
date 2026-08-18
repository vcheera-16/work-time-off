console.log('main.js loaded');

var currentUser = null;
var currentCalendarDate = new Date();
var appliedDates = [];
var holidays = [];

function readCookie(name) {
  const m = document.cookie.match('(^|;)\\s*' + name + '\\s*=\\s*([^;]+)');
  return m ? m.pop() : null;
}

function showError(msg) {
  var el = document.getElementById('errorArea');
  if (el) {
    el.textContent = msg;
    el.className = 'error';
    setTimeout(() => { el.textContent = ''; el.className = ''; }, 5000);
  }
}

function showSuccess(msg) {
  var el = document.getElementById('errorArea');
  if (el) {
    el.textContent = msg;
    el.className = 'success';
    setTimeout(() => { el.textContent = ''; el.className = ''; }, 3000);
  }
}

function setupTabs(user) {
  var tabNav = document.getElementById('tabNav');
  tabNav.innerHTML = '';
  
  var tabs = [
    { id: 'dashboardTab', label: 'Dashboard', show: true },
    { id: 'calendarTab', label: 'My Calendar', show: true },
    { id: 'applyTab', label: 'Apply Request', show: true },
    { id: 'teamTab', label: 'Team Requests', show: user.role === 'MANAGER' || user.role === 'ADMIN' },
    { id: 'historyTab', label: 'My Request History', show: true },
    { id: 'reportsTab', label: 'Reports', show: user.role === 'MANAGER' || user.role === 'ADMIN' },
    { id: 'usersTab', label: 'User Management', show: user.role === 'ADMIN' }
  ];

  tabs.forEach((tab, idx) => {
    if (tab.show) {
      var btn = document.createElement('button');
      btn.className = 'tab-btn' + (idx === 0 ? ' active' : '');
      btn.textContent = tab.label;
      btn.onclick = function(e) {
        e.preventDefault();
        switchTab(tab.id);
      };
      tabNav.appendChild(btn);
    }
  });
}

function switchTab(tabId) {
  document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
  document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
  
  document.getElementById(tabId).classList.add('active');
  event.target.classList.add('active');

  if (tabId === 'teamTab') loadTeamRequests();
  if (tabId === 'historyTab') loadMyRequests();
  if (tabId === 'reportsTab') setupReportForm();
  if (tabId === 'usersTab') loadUsers();
  if (tabId === 'calendarTab') renderCalendar();
}

$(function() {
  // Login
  $('#loginForm').on('submit', function(e) {
    e.preventDefault();
    var email = $(this).find('[name=email]').val();
    var password = $(this).find('[name=password]').val();
    
    $.ajax({
      url: '/api/auth/login',
      method: 'POST',
      contentType: 'application/json',
      data: JSON.stringify({ email: email, password: password })
    }).done(function(resp) {
      console.log('Login success', resp);
      showDashboard(resp);
    }).fail(function(xhr) {
      if (xhr.status === 401) showError('Invalid credentials');
      else if (xhr.status === 429) showError('Too many attempts. Try later.');
      else showError('Login failed');
    });
  });

  // Logout
  $(document).on('click', '#logoutBtn', function() {
    $.ajax({
      url: '/api/auth/logout',
      method: 'POST'
    }).always(function() {
      window.currentUser = null;
      $('#dashboard').hide();
      $('#loginPanel').show();
      $('#loginForm')[0].reset();
    });
  });

  // Apply for time off
  $(document).on('submit', '#applyForm', function(e) {
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
    }).done(function() {
      showSuccess('Time off request submitted successfully!');
      $('#applyForm')[0].reset();
      loadMyRequests();
    }).fail(function(xhr) {
      var resp = xhr.responseJSON;
      showError(resp && resp.error ? resp.error : 'Failed to submit request');
    });
  });

  // Team request approve/deny
  $(document).on('click', '.btn-approve', function() {
    var id = $(this).data('id');
    if (!confirm('Approve request #' + id + '?')) return;
    
    $.ajax({
      url: '/api/timeoff/' + id + '/review',
      method: 'POST',
      contentType: 'application/json',
      data: JSON.stringify({ action: 'APPROVE' })
    }).done(function() {
      showSuccess('Request approved!');
      loadTeamRequests();
    }).fail(function() {
      showError('Failed to approve request');
    });
  });

  $(document).on('click', '.btn-deny', function() {
    var id = $(this).data('id');
    var comment = prompt('Comment (optional):');
    if (comment === null) return;
    
    $.ajax({
      url: '/api/timeoff/' + id + '/review',
      method: 'POST',
      contentType: 'application/json',
      data: JSON.stringify({ action: 'DENY', comment: comment })
    }).done(function() {
      showSuccess('Request denied!');
      loadTeamRequests();
    }).fail(function() {
      showError('Failed to deny request');
    });
  });

  $(document).on('click', '.btn-cancel', function() {
    var id = $(this).data('id');
    if (!confirm('Cancel this request?')) return;
    
    $.ajax({
      url: '/api/timeoff/' + id + '/cancel',
      method: 'POST'
    }).done(function() {
      showSuccess('Request cancelled!');
      loadMyRequests();
    }).fail(function() {
      showError('Failed to cancel request');
    });
  });

  // Report generation
  $(document).on('submit', '#reportForm', function(e) {
    e.preventDefault();
    var startDate = $(this).find('[name=startDate]').val();
    var endDate = $(this).find('[name=endDate]').val();
    
    window.location.href = '/api/reports/pdf?startDate=' + startDate + '&endDate=' + endDate;
    showSuccess('Report downloading...');
  });

  // User management - create employee
  $(document).on('submit', '#createUserForm', function(e) {
    e.preventDefault();
    var payload = {
      email: $(this).find('[name=email]').val(),
      fullName: $(this).find('[name=fullName]').val(),
      managerId: parseInt($(this).find('[name=managerId]').val())
    };
    
    $.ajax({
      url: '/api/users',
      method: 'POST',
      contentType: 'application/json',
      data: JSON.stringify(payload)
    }).done(function() {
      showSuccess('Employee created successfully!');
      $('#createUserForm')[0].reset();
      loadUsers();
    }).fail(function(xhr) {
      var resp = xhr.responseJSON;
      showError(resp && resp.error ? resp.error : 'Failed to create employee');
    });
  });

  // Calendar navigation
  $(document).on('click', '#prevMonth', function() {
    currentCalendarDate.setMonth(currentCalendarDate.getMonth() - 1);
    renderCalendar();
  });

  $(document).on('click', '#nextMonth', function() {
    currentCalendarDate.setMonth(currentCalendarDate.getMonth() + 1);
    renderCalendar();
  });

  // Load initial data
  window.loadMyRequests = loadMyRequests;
  window.loadTeamRequests = loadTeamRequests;
  window.loadUsers = loadUsers;
  window.renderCalendar = renderCalendar;
  window.setupReportForm = setupReportForm;
});

function loadMyRequests() {
  $.getJSON('/api/timeoff').done(function(data) {
    var rows = data.map(function(r) {
      var actions = '';
      if (r.status === 'PENDING') {
        actions = '<button class="btn btn-cancel" data-id="' + r.id + '">Cancel</button>';
      }
      return [r.id, r.type, r.startDate, r.endDate, r.status, r.requestedAt ? r.requestedAt.substring(0, 10) : '', 
              r.reviewedByName || '-', r.managerComment || '-', actions];
    });
    
    if ($.fn.dataTable.isDataTable('#historyTable')) {
      $('#historyTable').DataTable().clear().rows.add(rows).draw();
    } else {
      $('#historyTable').DataTable({
        data: rows,
        columns: [
          { title: 'ID' }, { title: 'Type' }, { title: 'Start' }, { title: 'End' },
          { title: 'Status' }, { title: 'Requested At' }, { title: 'Reviewed By' },
          { title: 'Comment' }, { title: 'Actions', orderable: false }
        ]
      });
    }
  }).fail(function() {
    console.warn('Failed to load personal requests');
  });
}

function loadTeamRequests() {
  $.getJSON('/api/timeoff/team').done(function(data) {
    var rows = data.map(function(r) {
      var actions = '';
      if (r.status === 'PENDING') {
        actions = '<div class="btn-group"><button class="btn btn-approve" data-id="' + r.id + '">Approve</button> '
                + '<button class="btn btn-deny" data-id="' + r.id + '">Deny</button></div>';
      }
      return [r.id, r.userName || r.userEmail, r.type, r.startDate, r.endDate, r.status,
              r.userName || '-', r.reviewedByName || '-', actions];
    });
    
    if ($.fn.dataTable.isDataTable('#teamTable')) {
      $('#teamTable').DataTable().clear().rows.add(rows).draw();
    } else {
      $('#teamTable').DataTable({
        data: rows,
        columns: [
          { title: 'ID' }, { title: 'Employee' }, { title: 'Type' }, { title: 'Start' },
          { title: 'End' }, { title: 'Status' }, { title: 'Requested By' }, { title: 'Approved By' },
          { title: 'Actions', orderable: false }
        ]
      });
    }
  }).fail(function() {
    console.warn('Failed to load team requests');
  });
}

function loadUsers() {
  $.getJSON('/api/users').done(function(data) {
    var rows = data.map(function(u) {
      return [u.id, u.fullName || '-', u.email, u.role, u.managerId || '-', ''];
    });
    
    if ($.fn.dataTable.isDataTable('#usersTable')) {
      $('#usersTable').DataTable().clear().rows.add(rows).draw();
    } else {
      $('#usersTable').DataTable({
        data: rows,
        columns: [
          { title: 'ID' }, { title: 'Name' }, { title: 'Email' }, { title: 'Role' },
          { title: 'Manager ID' }, { title: 'Actions', orderable: false }
        ]
      });
    }
  }).fail(function() {
    console.warn('Failed to load users');
  });
}

function setupReportForm() {
  // Set default dates to this year
  var today = new Date();
  var startOfYear = new Date(today.getFullYear(), 0, 1);
  
  $('[name=startDate]').val(startOfYear.toISOString().split('T')[0]);
  $('[name=endDate]').val(today.toISOString().split('T')[0]);
}

function renderCalendar() {
  var year = currentCalendarDate.getFullYear();
  var month = currentCalendarDate.getMonth();
  
  $('#calendarMonth').text(currentCalendarDate.toLocaleDateString('en-US', { month: 'long', year: 'numeric' }));
  
  var container = document.getElementById('calendarContainer');
  container.innerHTML = '';
  
  var daysOfWeek = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  daysOfWeek.forEach(day => {
    var header = document.createElement('div');
    header.className = 'calendar-day header';
    header.textContent = day;
    container.appendChild(header);
  });
  
  var firstDay = new Date(year, month, 1).getDay();
  var daysInMonth = new Date(year, month + 1, 0).getDate();
  var daysInPrevMonth = new Date(year, month, 0).getDate();
  
  for (var i = firstDay - 1; i >= 0; i--) {
    var day = document.createElement('div');
    day.className = 'calendar-day other-month';
    day.textContent = daysInPrevMonth - i;
    container.appendChild(day);
  }
  
  var today = new Date();
  
  for (var d = 1; d <= daysInMonth; d++) {
    var day = document.createElement('div');
    var date = new Date(year, month, d);
    var dateStr = date.toISOString().split('T')[0];
    
    day.className = 'calendar-day';
    day.textContent = d;
    
    if (date.toDateString() === today.toDateString()) {
      day.classList.add('today');
    }
    
    var isHoliday = holidays.some(h => h.date === dateStr);
    var isApplied = appliedDates.includes(dateStr);
    var isPast = date < today;
    
    if (isHoliday) {
      day.classList.add('holiday');
      day.title = 'Federal Holiday - Cannot apply';
    } else if (isApplied) {
      day.classList.add('applied');
      day.title = 'Time off already applied';
    } else if (isPast) {
      day.classList.add('disabled');
      day.title = 'Past date';
    }
    
    container.appendChild(day);
  }
  
  for (var i = 1; i <= (42 - firstDay - daysInMonth); i++) {
    var day = document.createElement('div');
    day.className = 'calendar-day other-month';
    day.textContent = i;
    container.appendChild(day);
  }
}

function closePendingModal() {
  document.getElementById('pendingModal').classList.remove('active');
}
