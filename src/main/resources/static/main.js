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
    el.innerHTML = '<div class="error">' + msg + '</div>';
    setTimeout(() => { el.innerHTML = ''; }, 5000);
  }
}

function showSuccess(msg) {
  var el = document.getElementById('errorArea');
  if (el) {
    el.innerHTML = '<div class="success">' + msg + '</div>';
    setTimeout(() => { el.innerHTML = ''; }, 3000);
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
      btn.dataset.tabId = tab.id;
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
  
  // Find and activate button
  var btn = document.querySelector('.tab-btn[data-tabId="' + tabId + '"]');
  if (btn) btn.classList.add('active');
  
  document.getElementById(tabId).classList.add('active');
  
  // Hide welcome message if not on dashboard
  var welcomeSection = document.getElementById('welcomeSection');
  if (tabId === 'dashboardTab') {
    welcomeSection.style.display = 'block';
  } else {
    welcomeSection.style.display = 'none';
  }

  // Trigger data loading for specific tabs
  if (tabId === 'teamTab') {
    setTimeout(() => loadTeamRequests(), 100);
  }
  if (tabId === 'historyTab') {
    setTimeout(() => loadMyRequests(), 100);
  }
  if (tabId === 'reportsTab') {
    setupReportForm();
  }
  if (tabId === 'usersTab') {
    setTimeout(() => loadUsers(), 100);
  }
  if (tabId === 'calendarTab') {
    setTimeout(() => renderCalendar(), 100);
  }
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
    }).fail(function(xhr) {
      var resp = xhr.responseJSON;
      showError(resp && resp.error ? resp.error : 'Failed to approve request');
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
    }).fail(function(xhr) {
      var resp = xhr.responseJSON;
      showError(resp && resp.error ? resp.error : 'Failed to deny request');
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
    }).fail(function(xhr) {
      var resp = xhr.responseJSON;
      showError(resp && resp.error ? resp.error : 'Failed to cancel request');
    });
  });

  // Report generation
  $(document).on('submit', '#reportForm', function(e) {
    e.preventDefault();
    var startDate = $(this).find('[name=startDate]').val();
    var endDate = $(this).find('[name=endDate]').val();
    var statuses = [];
    $(this).find('[name=status]:checked').each(function() {
      statuses.push($(this).val());
    });
    
    if (statuses.length === 0) {
      showError('Please select at least one status');
      return;
    }
    
    var statusStr = statuses.join('&status=');
    var url = '/api/reports/pdf?startDate=' + startDate + '&endDate=' + endDate + '&status=' + statusStr;
    window.location.href = url;
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
  console.log('Loading my requests...');
  $.getJSON('/api/timeoff').done(function(data) {
    console.log('My requests data:', data);
    var rows = data.map(function(r) {
      var actions = '';
      if (r.status === 'PENDING') {
        actions = '<button class="btn btn-cancel" data-id="' + r.id + '">Cancel</button>';
      }
      return [r.id, r.type, r.startDate, r.endDate, r.status, r.requestedAt ? r.requestedAt.substring(0, 10) : '', 
              r.reviewedByName || '-', r.managerComment || '-', actions];
    });
    
    if ($.fn.dataTable.isDataTable('#historyTable')) {
      $('#historyTable').DataTable().destroy();
    }
    $('#historyTable').DataTable({
      data: rows,
      columns: [
        { title: 'ID' }, { title: 'Type' }, { title: 'Start' }, { title: 'End' },
        { title: 'Status' }, { title: 'Requested At' }, { title: 'Reviewed By' },
        { title: 'Comment' }, { title: 'Actions', orderable: false }
      ]
    });
  }).fail(function(xhr) {
    console.error('Failed to load personal requests', xhr);
    showError('Failed to load your requests');
  });
}

function loadTeamRequests() {
  console.log('Loading team requests...');
  $.getJSON('/api/timeoff/team').done(function(data) {
    console.log('Team requests API response:', data);
    var rows = data.map(function(r) {
      console.log('Processing request:', r);
      var actions = '';
      if (r.status === 'PENDING') {
        actions = '<div class="btn-group"><button class="btn btn-approve" data-id="' + r.id + '">Approve</button> '
                + '<button class="btn btn-deny" data-id="' + r.id + '">Deny</button></div>';
      }
      return [r.id, r.userName || r.userEmail || 'Unknown', r.type, r.startDate, r.endDate, r.status,
              r.requestedAt ? r.requestedAt.substring(0, 10) : '-', r.reviewedByName || '-', actions];
    });
    
    console.log('Rows to display:', rows);
    
    if ($.fn.dataTable.isDataTable('#teamTable')) {
      $('#teamTable').DataTable().destroy();
    }
    var dt = $('#teamTable').DataTable({
      data: rows,
      columns: [
        { title: 'ID' }, { title: 'Employee' }, { title: 'Type' }, { title: 'Start' },
        { title: 'End' }, { title: 'Status' }, { title: 'Requested' }, { title: 'Approved By' },
        { title: 'Actions', orderable: false }
      ],
      columnDefs: [
        { targets: -1, render: function(data) { return data; } }
      ]
    });
    console.log('DataTable created with', rows.length, 'rows');
  }).fail(function(xhr) {
    console.error('Failed to load team requests', xhr);
    showError('Failed to load team requests');
  });
}

function loadUsers() {
  console.log('Loading users...');
  $.getJSON('/api/users').done(function(data) {
    var rows = data.map(function(u) {
      return [u.id, u.fullName || '-', u.email, u.role, u.managerId || '-'];
    });
    
    if ($.fn.dataTable.isDataTable('#usersTable')) {
      $('#usersTable').DataTable().destroy();
    }
    $('#usersTable').DataTable({
      data: rows,
      columns: [
        { title: 'ID' }, { title: 'Name' }, { title: 'Email' }, { title: 'Role' },
        { title: 'Manager ID' }
      ]
    });
  }).fail(function(xhr) {
    console.error('Failed to load users', xhr);
    showError('Failed to load users');
  });
}

function setupReportForm() {
  console.log('Setting up report form...');
  var today = new Date();
  var startOfYear = new Date(today.getFullYear(), 0, 1);
  
  document.getElementById('reportStartDate').value = startOfYear.toISOString().split('T')[0];
  document.getElementById('reportEndDate').value = today.toISOString().split('T')[0];
}

function renderCalendar() {
  console.log('Rendering calendar for:', currentCalendarDate);
  
  // Load applied dates first
  $.getJSON('/api/timeoff').done(function(data) {
    window.appliedDates = data
      .filter(r => r.status === 'APPROVED' || r.status === 'PENDING')
      .flatMap(r => {
        var dates = [];
        var start = new Date(r.startDate);
        var end = new Date(r.endDate);
        for (var d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
          dates.push(d.toISOString().split('T')[0]);
        }
        return dates;
      });
    
    renderCalendarUI();
  });
}

function renderCalendarUI() {
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
  
  // Add empty cells for days before month starts
  for (var i = 0; i < firstDay; i++) {
    var emptyDay = document.createElement('div');
    emptyDay.className = 'calendar-day other-month';
    container.appendChild(emptyDay);
  }
  
  var today = new Date();
  today.setHours(0, 0, 0, 0);
  
  for (var d = 1; d <= daysInMonth; d++) {
    var day = document.createElement('div');
    var date = new Date(year, month, d);
    var dateStr = date.toISOString().split('T')[0];
    
    day.className = 'calendar-day';
    day.textContent = d;
    
    if (date.getTime() === today.getTime()) {
      day.classList.add('today');
      day.title = 'Today';
    }
    
    var isHoliday = holidays.some(h => h.date === dateStr);
    var isApplied = appliedDates.includes(dateStr);
    var isPast = date < today;
    
    if (isHoliday) {
      day.classList.add('holiday');
      day.title = 'Federal Holiday';
    } else if (isApplied) {
      day.classList.add('applied');
      day.title = 'Time off applied';
    } else if (isPast) {
      day.classList.add('disabled');
      day.title = 'Past date';
    }
    
    container.appendChild(day);
  }
}

function closePendingModal() {
  document.getElementById('pendingModal').classList.remove('active');
}
