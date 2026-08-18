function showDashboard(user) {
  try {
    // keep a global reference to the logged-in user
    window.currentUser = user;

    // hide login panel and show dashboard
    var lp = document.getElementById('loginPanel');
    var db = document.getElementById('dashboard');
    if (lp) lp.style.display = 'none';
    if (db) db.style.display = 'block';

    // render a simple welcome area
    var sd = document.getElementById('showDashboard');
    if (sd) {
      sd.innerHTML = '<strong>Welcome, ' + (user.fullName || user.email) + '</strong>' +
        '<div class="small">Role: ' + (user.role || '') + '</div>';
    }

    // load personal requests
    if (window._loadMyRequests) window._loadMyRequests();

    // if manager, show team section and load team requests
    if (user.role === 'MANAGER') {
      var ms = document.getElementById('managerSection');
      if (ms) ms.style.display = 'block';
      if (window._loadTeamRequests) window._loadTeamRequests();
    }

    console.log('Dashboard shown for', user);
  } catch (e) {
    console.error('showDashboard error', e);
  }
}
