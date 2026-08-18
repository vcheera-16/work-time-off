function showDashboard(user) {
  try {
    window.currentUser = user;
    document.getElementById('loginPanel').style.display = 'none';
    document.getElementById('dashboard').style.display = 'block';
    
    // Render welcome section
    var welcomeHTML = '<div class="welcome-msg"><h2>Welcome, ' + (user.fullName || user.email) + '!</h2><p>Role: <strong>' + user.role + '</strong></p></div>';
    document.getElementById('welcomeSection').innerHTML = welcomeHTML;
    
    // Setup tabs based on role
    setupTabs(user);
    
    // Load initial data
    loadDashboardStats();
    loadCalendarData();
    
    console.log('Dashboard shown for', user);
  } catch (e) {
    console.error('showDashboard error', e);
  }
}

function loadDashboardStats() {
  $.getJSON('/api/dashboard/stats').done(function(stats) {
    var statsHTML = '<div class="stats-grid">';
    statsHTML += '<div class="stat-card"><h3>Available PTOs</h3><div class="value">' + stats.availablePTOs + '<span class="unit">days</span></div></div>';
    statsHTML += '<div class="stat-card"><h3>Used PTOs</h3><div class="value">' + stats.usedPTOs + '<span class="unit">days</span></div></div>';
    statsHTML += '<div class="stat-card"><h3>Total PTOs</h3><div class="value">' + stats.totalPTOs + '<span class="unit">days</span></div></div>';
    statsHTML += '<div class="stat-card clickable" onclick="openPendingModal()"><h3>Pending Approvals</h3><div class="value">' + stats.pendingApprovals + '</div></div>';
    statsHTML += '</div>';
    
    document.getElementById('statsContainer').innerHTML = statsHTML;
  });
  
  $.getJSON('/api/dashboard/holidays').done(function(holidays_data) {
    window.holidays = holidays_data;
    
    var holidaysHTML = '<div style="background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);"><h3>Upcoming Federal Holidays</h3><ul style="list-style: none; padding: 0;">';
    var today = new Date().toISOString().split('T')[0];
    var upcoming = holidays_data.filter(h => h.date >= today).slice(0, 5);
    
    if (upcoming.length === 0) {
      holidaysHTML += '<li>No upcoming holidays</li>';
    } else {
      upcoming.forEach(h => {
        holidaysHTML += '<li style="padding: 8px 0; border-bottom: 1px solid #f0f0f0;"><strong>' + h.name + ':</strong> ' + h.date + '</li>';
      });
    }
    
    holidaysHTML += '</ul></div>';
    document.getElementById('holidaysContainer').innerHTML = holidaysHTML;
  });
}

function loadCalendarData() {
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
  });
}

function openPendingModal() {
  var modal = document.getElementById('pendingModal');
  modal.classList.add('active');
  
  $.getJSON('/api/timeoff/pending').done(function(data) {
    var rows = data.map(function(r) {
      return [r.id, r.type, r.startDate, r.endDate, r.requestedAt ? r.requestedAt.substring(0, 10) : '', r.status];
    });
    
    if ($.fn.dataTable.isDataTable('#pendingTable')) {
      $('#pendingTable').DataTable().clear().rows.add(rows).draw();
    } else {
      $('#pendingTable').DataTable({
        data: rows,
        columns: [
          { title: 'ID' }, { title: 'Type' }, { title: 'Start' }, { title: 'End' },
          { title: 'Requested At' }, { title: 'Status' }
        ]
      });
    }
  });
}

window.closePendingModal = closePendingModal;
window.openPendingModal = openPendingModal;
window.loadDashboardStats = loadDashboardStats;
window.loadCalendarData = loadCalendarData;
