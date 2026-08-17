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
        '<div>Role: ' + (user.role || '') + '</div>';
    }

    // TODO: populate teamTable with real data via API calls
    console.log('Dashboard shown for', user);
  } catch (e) {
    console.error('showDashboard error', e);
  }
}
