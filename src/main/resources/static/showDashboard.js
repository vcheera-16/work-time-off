function showDashboard(user) {
  try {
    window.currentUser = user;
    document.getElementById('loginPanel').style.display = 'none';
    document.getElementById('dashboard').style.display = 'block';
    
    // Render welcome section - only on dashboard tab
    var welcomeHTML = '<div class="welcome-msg"><h2>Welcome, ' + (user.fullName || user.email) + '! 👋</h2><p>Role: <strong>' + user.role + '</strong></p></div>';
    document.getElementById('welcomeSection').innerHTML = welcomeHTML;
    
    // Setup tabs based on role
    setupTabs(user);
    
    // Load initial data
    loadDashboardStats();
    loadCalendarData();
    loadManagersList();
    
    console.log('Dashboard shown for', user);
  } catch (e) {
    console.error('showDashboard error', e);
  }
}

function loadDashboardStats() {
  $.getJSON('/api/dashboard/stats').done(function(stats) {
    var statsHTML = '<div class="stats-grid">';
    
    // Available PTOs with speedometer
    var usedPercent = stats.usedPTOs > 0 ? Math.round((stats.usedPTOs / stats.totalPTOs) * 100) : 0;
    var availPercent = 100 - usedPercent;
    statsHTML += '<div class="stat-card"><h3>Available PTOs</h3>';
    statsHTML += '<div class="speedometer-container"><div class="speedometer"><div class="speedometer-needle" style="transform: rotate(' + (availPercent * 1.8) + 'deg);"></div></div></div>';
    statsHTML += '<div class="value">' + stats.availablePTOs + '</div><div class="unit">out of ' + stats.totalPTOs + ' days</div>';
    statsHTML += '</div>';
    
    // Used PTOs with speedometer
    statsHTML += '<div class="stat-card"><h3>Used PTOs</h3>';
    statsHTML += '<div class="speedometer-container"><div class="speedometer"><div class="speedometer-needle" style="transform: rotate(' + (usedPercent * 1.8) + 'deg);"></div></div></div>';
    statsHTML += '<div class="value">' + stats.usedPTOs + '</div><div class="unit">' + usedPercent + '% utilized</div>';
    statsHTML += '</div>';
    
    // Total PTOs
    statsHTML += '<div class="stat-card"><h3>Total PTOs</h3><div class="value">' + stats.totalPTOs + '<span class="unit">days</span></div></div>';
    
    // Pending Approvals clickable
    statsHTML += '<div class="stat-card clickable" onclick="openPendingModal()"><h3>Pending Approvals</h3><div class="value">' + stats.pendingApprovals + '</div><div class="unit" style="font-size: 12px; margin-top: 10px; color: #667eea;">Click to view</div></div>';
    
    statsHTML += '</div>';
    document.getElementById('statsContainer').innerHTML = statsHTML;
  });
  
  $.getJSON('/api/dashboard/holidays').done(function(holidays_data) {
    window.holidays = holidays_data;
    
    var today = new Date().toISOString().split('T')[0];
    var upcoming = holidays_data.filter(h => h.date >= today).slice(0, 6);
    
    var holidaysHTML = '<div><h2 style="margin-top: 40px; color: #333;">🎉 Upcoming Federal Holidays</h2>';
    
    if (upcoming.length === 0) {
      holidaysHTML += '<p style="color: #666;">No upcoming holidays</p>';
    } else {
      holidaysHTML += '<div class="holidays-grid">';
      var holidays_icons = {
        'Christmas': '🎄',
        'New Year': '🎆',
        'Independence Day': '🇺🇸',
        'Thanksgiving': '🦃',
        'Labor Day': '👷',
        'Veterans Day': '🎖️',
        'Memorial Day': '🇺🇸',
        'Columbus Day': '🧭',
        'MLK Jr': '✊',
        'Presidents Day': '👔'
      };
      
      upcoming.forEach(h => {
        var icon = '📅';
        for (var key in holidays_icons) {
          if (h.name.indexOf(key) !== -1) {
            icon = holidays_icons[key];
            break;
          }
        }
        
        var dateObj = new Date(h.date);
        var formattedDate = dateObj.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
        
        holidaysHTML += '<div class="holiday-card">';
        holidaysHTML += '<div class="holiday-icon">' + icon + '</div>';
        holidaysHTML += '<div class="holiday-info">';
        holidaysHTML += '<h4>' + h.name + '</h4>';
        holidaysHTML += '<p>' + formattedDate + '</p>';
        holidaysHTML += '</div></div>';
      });
      
      holidaysHTML += '</div>';
    }
    
    holidaysHTML += '</div>';
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

function loadManagersList() {
  $.getJSON('/api/users/managers').done(function(managers) {
    var select = document.querySelector('select[name="managerId"]');
    if (select) {
      select.innerHTML = '<option value="">-- Select Manager --</option>';
      managers.forEach(m => {
        var option = document.createElement('option');
        option.value = m.id;
        option.textContent = m.fullName || m.email;
        select.appendChild(option);
      });
    }
  });
}

function openPendingModal() {
  var modal = document.getElementById('pendingModal');
  modal.classList.add('active');
  
  $.getJSON('/api/timeoff/pending').done(function(data) {
    var rows = data.map(function(r) {
      return [r.id, r.userName || r.userEmail || 'Unknown', r.type, r.startDate, r.endDate, r.requestedAt ? r.requestedAt.substring(0, 10) : '', r.status];
    });
    
    if ($.fn.dataTable.isDataTable('#pendingTable')) {
      $('#pendingTable').DataTable().destroy();
    }
    $('#pendingTable').DataTable({
      data: rows,
      columns: [
        { title: 'ID' }, { title: 'Employee' }, { title: 'Type' }, { title: 'Start' }, { title: 'End' },
        { title: 'Requested' }, { title: 'Status' }
      ]
    });
  });
}

window.closePendingModal = closePendingModal;
window.openPendingModal = openPendingModal;
window.loadDashboardStats = loadDashboardStats;
window.loadCalendarData = loadCalendarData;
