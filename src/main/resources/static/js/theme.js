// Page dikhne se PEHLE chalti hai, taaki dark mode wale user ko pehle light flash na dikhe
(function () {
  var theme = null;
  try { theme = localStorage.getItem('automation-hub-theme'); } catch (e) { /* storage band ho toh ignore */ }

  // User ne kuch chuna nahi toh computer ki theme follow karo
  var systemDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
  document.documentElement.dataset.theme = theme || (systemDark ? 'dark' : 'light');
})();