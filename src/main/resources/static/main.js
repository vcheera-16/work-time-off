console.log('main.js loaded');

var currentUser = null;
var currentCalendarDate = new Date();
var appliedDates = [];
var holidays = [];

// Federal holidays for the US (fixed + rule-based floating holidays)
function getFederalHolidayDates(year) {
  var dates = new Set();

  function pad(n) { return String(n).padStart(2, '0'); }
  function fmt(y, m, d) { return y + '-' + pad(m) + '-' + pad(d); }

  // Observed date helper: if date falls on Sat -> Fri, Sun -> Mon
  function observed(y, m, d) {
    var dt = new Date(y, m - 1, d);
    var dow = dt.getDay();
    if (dow === 6) return new Date(y, m - 1, d - 1); // Sat -> Fri
    if (dow === 0) return new Date(y, m - 1, d + 1); // Sun -> Mon
    return dt;
  }

  // nth weekday of month: e.g. nthWeekday(year, 1, 1, 3) = 3rd Monday of Jan
  function nthWeekday(y, month, weekday, n) {
    var first = new Date(y, month - 1, 1);
    var diff = (weekday - first.getDay() + 7) % 7;
    return new Date(y, month - 1, 1 + diff + (n - 1) * 7);
  }

  // Last weekday of month
  function lastWeekday(y, month, weekday) {
    var last = new Date(y, month, 0);
    var diff = (last.getDay() - weekday + 7) % 7;
    return new Date(y, month - 1, last.getDate() - diff);
  }

  function addObserved(y, m, d) {
    var obs = observed(y, m, d);
    dates.add(fmt(obs.getFullYear(), obs.getMonth() + 1, obs.getDate()));
  }

  function addDate(dt) {
    dates.add(fmt(dt.getFullYear(), dt.getMonth() + 1, dt.getDate()));
  }

  // New Year's Day - Jan 1
  addObserved(year, 1, 1);
  // Birthday of Martin Luther King Jr. - 3rd Monday in January
  addDate(nthWeekday(year, 1, 1, 3));
  // Washington's Birthday - 3rd Monday in February
  addDate(nthWeekday(year, 2, 1, 3));
  // Memorial Day - last Monday in May
  addDate(lastWeekday(year, 5, 1));
  // Juneteenth - Jun 19
  addObserved(year, 6, 19);
  // Independence Day - Jul 4
  addObserved(year, 7, 4);
  // Labor Day - 1st Monday in September
  addDate(nthWeekday(year, 9, 1, 1));
  // Columbus Day - 2nd Monday in October
  addDate(nthWeekday(year, 10, 1, 2));
  // Veterans Day - Nov 11
  addObserved(year, 11, 11);
  // Thanksgiving Day - 4th Thursday in November
  addDate(nthWeekday(year, 11, 4, 4));
  // Christmas Day - Dec 25
  addObserved(year, 12, 25);

  return dates;
}

// Check if a date string (YYYY-MM-DD) is a weekend or federal holiday
function isWeekendOrHoliday(dateStr) {
  var dt = new Date(dateStr + 'T00:00:00');
  var dow = dt.getDay();
  if (dow === 0 || dow === 6) return true; // Sunday or Saturday
  var holidayDates = getFederalHolidayDates(dt.getFullYear());
  return holidayDates.has(dateStr);
}

// Count business days between two date strings (inclusive), excluding weekends and federal holidays
function countBusinessDays(startStr, endStr) {
  var start = new Date(startStr + 'T00:00:00');
  var end = new Date(endStr + 'T00:00:00');
  var count = 0;
  for (var d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
    var mm = String(d.getMonth() + 1).padStart(2, '0');
    var dd = String(d.getDate()).padStart(2, '0');
    var ds = d.getFullYear() + '-' + mm + '-' + dd;
    if (!isWeekendOrHoliday(ds)) count++;
  }
  return count;
}

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
  if (tabId === 'dashboardTab') {
    loadDashboardStats();
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
    var startDate = $(this).find('[name=startDate]').val();
    var endDate = $(this).find('[name=endDate]').val();
    var type = $(this).find('[name=type]').val();

    if (!startDate || !endDate) {
      showError('Please select both start and end date.');
      return;
    }
    if (isWeekendOrHoliday(startDate)) {
      showError('Start date cannot be a weekend or federal holiday.');
      return;
    }
    if (isWeekendOrHoliday(endDate)) {
      showError('End date cannot be a weekend or federal holiday.');
      return;
    }
    if (endDate < startDate) {
      showError('End date cannot be before start date.');
      return;
    }

    var businessDays = countBusinessDays(startDate, endDate);
    if (businessDays === 0) {
      showError('Selected date range contains no working days (all weekends or holidays).');
      return;
    }

    var payload = {
      type: type,
      startDate: startDate,
      endDate: endDate
    };
    
    $.ajax({
      url: '/api/timeoff',
      method: 'POST',
      contentType: 'application/json',
      data: JSON.stringify(payload)
    }).done(function() {
      showSuccess('Time off request submitted successfully! (' + businessDays + ' working day(s) counted, weekends and federal holidays excluded)');
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

    if (!startDate || !endDate) {
      showError('Please select start and end dates');
      return;
    }
    if (statuses.length === 0) {
      showError('Please select at least one status');
      return;
    }

    var params = 'startDate=' + startDate + '&endDate=' + endDate;
    statuses.forEach(function(s) { params += '&status=' + s; });

    $.getJSON('/api/reports/data?' + params).done(function(data) {
      var rows = data.map(function(r) {
        return [
          r.userName || r.userEmail || '-',
          r.type,
          r.startDate,
          r.endDate,
          r.status,
          r.requestedAt ? r.requestedAt.substring(0, 10) : '-'
        ];
      });

      if ($.fn.dataTable.isDataTable('#reportTable')) {
        $('#reportTable').DataTable().destroy();
      }
      $('#reportTable').DataTable({
        data: rows,
        columns: [
          { title: 'Employee' }, { title: 'Type' }, { title: 'Start Date' },
          { title: 'End Date' }, { title: 'Status' }, { title: 'Requested Date' }
        ]
      });

      if (data.length === 0) {
        showError('No records found for the selected criteria');
      } else {
        showSuccess('Report generated: ' + data.length + ' record(s) found');
      }
    }).fail(function(xhr) {
      var resp = xhr.responseJSON;
      showError(resp && resp.error ? resp.error : 'Failed to generate report');
    });
  });

  // Role dropdown change - show/hide manager dropdown label
  $(document).on('change', '#newUserRole', function() {
    var role = $(this).val();
    var managerLabel = $('#newUserManager').closest('label');
    if (role === 'MANAGER') {
      // For managers, manager selection is optional
      $('#newUserManager').prop('required', false);
      managerLabel.find('span').text('Manager (optional):');
    } else {
      $('#newUserManager').prop('required', true);
      managerLabel.find('span').text('Manager:');
    }
  });

  // User management - create user
  $(document).on('submit', '#createUserForm', function(e) {
    e.preventDefault();
    var managerIdVal = $(this).find('[name=managerId]').val();
    var payload = {
      email: $(this).find('[name=email]').val(),
      fullName: $(this).find('[name=fullName]').val(),
      role: $(this).find('[name=role]').val(),
      managerId: managerIdVal ? parseInt(managerIdVal) : null
    };
    
    $.ajax({
      url: '/api/users',
      method: 'POST',
      contentType: 'application/json',
      data: JSON.stringify(payload)
    }).done(function() {
      showSuccess('User created successfully!');
      $('#createUserForm')[0].reset();
      loadUsers();
    }).fail(function(xhr) {
      var resp = xhr.responseJSON;
      showError(resp && resp.error ? resp.error : 'Failed to create user');
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
    var isAdmin = window.currentUser && window.currentUser.role === 'ADMIN';
    var rows = data.map(function(u) {
      var row = [u.id, u.fullName || '-', u.email, u.role, u.managerId || '-'];
      if (isAdmin) {
        row.push('<button class="btn-delete-user" data-id="' + u.id + '" data-name="' + (u.fullName || u.email) + '">Delete</button>');
      }
      return row;
    });

    if ($.fn.dataTable.isDataTable('#usersTable')) {
      $('#usersTable').DataTable().destroy();
    }
    var columns = [
      { title: 'ID' }, { title: 'Name' }, { title: 'Email' }, { title: 'Role' },
      { title: 'Manager ID' }
    ];
    if (isAdmin) {
      columns.push({ title: 'Actions' });
    }
    $('#usersTable').DataTable({
      data: rows,
      columns: columns,
      columnDefs: isAdmin ? [{ targets: -1, orderable: false }] : []
    });

    // Populate the manager dropdown for the create user form
    var managerSel = document.getElementById('newUserManager');
    if (managerSel) {
      managerSel.innerHTML = '<option value="">-- Select Manager --</option>';
      data.forEach(function(u) {
        var opt = document.createElement('option');
        opt.value = u.id;
        opt.textContent = (u.fullName || u.email) + ' (' + u.role + ')';
        managerSel.appendChild(opt);
      });
    }
  }).fail(function(xhr) {
    console.error('Failed to load users', xhr);
    showError('Failed to load users');
  });
}

// Handle delete user button click
$(document).on('click', '.btn-delete-user', function() {
  var userId = $(this).data('id');
  var userName = $(this).data('name');
  if (!confirm('Are you sure you want to delete user "' + userName + '"? This action cannot be undone.')) {
    return;
  }
  var csrf = readCookie('XSRF-TOKEN');
  $.ajax({
    url: '/api/users/' + userId,
    method: 'DELETE',
    headers: csrf ? { 'X-XSRF-TOKEN': csrf } : {}
  }).done(function() {
    showSuccess('User deleted successfully.');
    loadUsers();
  }).fail(function(xhr) {
    var msg = (xhr.responseJSON && xhr.responseJSON.error) ? xhr.responseJSON.error : 'Failed to delete user';
    showError(msg);
  });
});

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
    window.appliedDates = [];
    window.appliedDateTypes = {};
    data.filter(r => r.status === 'APPROVED' || r.status === 'PENDING')
      .forEach(r => {
        var start = new Date(r.startDate + 'T00:00:00');
        var end = new Date(r.endDate + 'T00:00:00');
        for (var d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
          var ds = d.toISOString().split('T')[0];
          window.appliedDates.push(ds);
          window.appliedDateTypes[ds] = r.type;
        }
      });
    
    renderCalendarUI();
  });
}

function getHolidaySymbol(name) {
  var symbols = {
    'Christmas': '🎄',
    'New Year': '🎆',
    'Independence Day': '🇺🇸',
    'Thanksgiving': '🦃',
    'Labor Day': '👷',
    'Veterans Day': '🏅',
    "Veterans' Day": '🏅',
    "Veteran's Day": '🏅',
    'Memorial Day': '🇺🇸',
    'Columbus Day': '🧭',
    'MLK': '✊',
    'Martin Luther King': '✊',
    'Presidents Day': '👔',
    "Presidents' Day": '👔',
    'Washington': '👔',
    'Juneteenth': '✊'
  };
  for (var key in symbols) {
    if (name.indexOf(key) !== -1) return symbols[key];
  }
  return '🗓️';
}

function renderCalendarUI() {
  var year = currentCalendarDate.getFullYear();
  var month = currentCalendarDate.getMonth();
  
  $('#calendarMonth').text(currentCalendarDate.toLocaleDateString('en-US', { month: 'long', year: 'numeric' }));
  
  var container = document.getElementById('calendarContainer');
  container.innerHTML = '';
  
  var daysOfWeek = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  daysOfWeek.forEach(function(d) {
    var header = document.createElement('div');
    header.className = 'calendar-day header';
    header.textContent = d;
    container.appendChild(header);
  });
  
  // Use local date constructor to avoid timezone shifting
  var firstDayDate = new Date(year, month, 1);
  var firstDay = firstDayDate.getDay(); // 0=Sun, 1=Mon, ...
  var daysInMonth = new Date(year, month + 1, 0).getDate();
  
  // Add empty/placeholder cells for days before month starts
  for (var i = 0; i < firstDay; i++) {
    var emptyDay = document.createElement('div');
    emptyDay.className = 'calendar-day other-month';
    container.appendChild(emptyDay);
  }
  
  var today = new Date();
  today.setHours(0, 0, 0, 0);
  
  // Build holiday lookup by date string
  var holidayMap = {};
  (window.holidays || []).forEach(function(h) {
    holidayMap[h.date] = h.name;
  });
  
  for (var d = 1; d <= daysInMonth; d++) {
    var dayEl = document.createElement('div');
    var date = new Date(year, month, d);
    var mm = String(month + 1).padStart(2, '0');
    var dd = String(d).padStart(2, '0');
    var dateStr = year + '-' + mm + '-' + dd;
    
    dayEl.className = 'calendar-day';
    
    var numSpan = document.createElement('span');
    numSpan.className = 'calendar-day-num';
    numSpan.textContent = d;
    dayEl.appendChild(numSpan);
    
    if (date.getTime() === today.getTime()) {
      dayEl.classList.add('today');
      dayEl.title = 'Today';
    }
    
    var holidayName = holidayMap[dateStr];
    var isApplied = appliedDates.includes(dateStr);
    var isPast = date < today;
    var appliedType = window.appliedDateTypes ? window.appliedDateTypes[dateStr] : null;
    var isWeekend = (date.getDay() === 0 || date.getDay() === 6);
    var isFedHoliday = !holidayName && getFederalHolidayDates(year).has(dateStr);

    if (isWeekend) {
      dayEl.classList.add('weekend-disabled');
      dayEl.title = 'Weekends are not counted as PTO';
    } else if (holidayName) {
      dayEl.classList.add('holiday');
      var sym = document.createElement('span');
      sym.className = 'calendar-day-sym';
      sym.textContent = getHolidaySymbol(holidayName);
      dayEl.appendChild(sym);
      dayEl.title = holidayName + ' (Federal Holiday – not counted as PTO)';
    } else if (isFedHoliday) {
      dayEl.classList.add('holiday');
      dayEl.title = 'Federal Holiday – not counted as PTO';
    } else if (isApplied) {
      if (appliedType === 'VACATION') {
        dayEl.classList.add('applied-vacation');
        var sym2 = document.createElement('span');
        sym2.className = 'calendar-day-sym';
        sym2.textContent = '☀️';
        dayEl.appendChild(sym2);
        dayEl.title = 'Vacation';
      } else if (appliedType === 'SICK') {
        dayEl.classList.add('applied-sick');
        var sym3 = document.createElement('span');
        sym3.className = 'calendar-day-sym';
        sym3.textContent = '🤒';
        dayEl.appendChild(sym3);
        dayEl.title = 'Sick Day';
      } else {
        dayEl.classList.add('applied');
        var sym4 = document.createElement('span');
        sym4.className = 'calendar-day-sym';
        sym4.textContent = '📋';
        dayEl.appendChild(sym4);
        dayEl.title = 'Time off applied';
      }
    } else if (isPast) {
      dayEl.classList.add('disabled');
      dayEl.title = 'Past date';
    }
    
    container.appendChild(dayEl);
  }
}

function closePendingModal() {
  document.getElementById('pendingModal').classList.remove('active');
}

function downloadReportPdf() {
  if (!$.fn.dataTable.isDataTable('#reportTable')) {
    showError('Please generate a report first before downloading.');
    return;
  }
  var dt = $('#reportTable').DataTable();
  var headers = ['Employee', 'Type', 'Start Date', 'End Date', 'Status', 'Requested Date'];
  var rows = dt.rows({ search: 'applied' }).data().toArray();
  if (rows.length === 0) {
    showError('No report data to download.');
    return;
  }

  var { jsPDF } = window.jspdf;
  var doc = new jsPDF({ orientation: 'landscape' });
  doc.setFontSize(16);
  doc.text('Time Off Report', 14, 15);
  doc.setFontSize(10);
  doc.text('Generated: ' + new Date().toLocaleDateString(), 14, 22);

  doc.autoTable({
    startY: 28,
    head: [headers],
    body: rows,
    styles: { fontSize: 9, cellPadding: 3 },
    headStyles: { fillColor: [102, 126, 234], textColor: 255, fontStyle: 'bold' },
    alternateRowStyles: { fillColor: [245, 245, 255] }
  });

  doc.save('time-off-report.pdf');
}
