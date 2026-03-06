---
title: Leaderboard
layout: wide
toc: false
---

<style>
article > h1.hx\:text-center { display: none; }
article > br { display: none; }
article { max-width: 100% !important; }
</style>

<div style="margin-bottom:1.5rem;">
<h1 class="not-prose hx:text-4xl hx:font-bold hx:leading-none hx:tracking-tighter hx:md:text-5xl hx:py-2 hx:bg-clip-text hx:text-transparent hx:bg-gradient-to-r hx:from-gray-900 hx:to-gray-600 hx:dark:from-gray-100 hx:dark:to-gray-400">Leaderboard</h1>
<div id="http-version-tabs" style="display:flex; gap:0.5rem; margin-top:0.75rem;">
<span class="http-ver active" data-ver="h1" style="display:inline-block; padding:0.3rem 0.85rem; font-size:0.8rem; font-weight:600; border-radius:4px; background:rgba(59,130,246,0.12); color:#2563eb; border:1.5px solid #3b82f6; cursor:pointer; transition:all 0.15s ease;">HTTP/1.1</span>
<span class="http-ver" data-ver="h2" style="display:inline-block; padding:0.3rem 0.85rem; font-size:0.8rem; font-weight:600; border-radius:4px; background:rgba(0,0,0,0.03); color:#94a3b8; border:1.5px solid #e2e8f0; cursor:pointer; transition:all 0.15s ease;">HTTP/2</span>
<span class="http-ver" data-ver="h3" style="display:inline-block; padding:0.3rem 0.85rem; font-size:0.8rem; font-weight:600; border-radius:4px; background:rgba(0,0,0,0.03); color:#94a3b8; border:1.5px solid #e2e8f0; cursor:pointer; transition:all 0.15s ease;">HTTP/3</span>
</div>
<div id="http-coming-soon" style="display:none; margin-top:1rem; padding:1.25rem 1.5rem; border-radius:8px; border:1.5px solid #e2e8f0; background:rgba(255,255,255,0.85); box-shadow:0 2px 8px rgba(0,0,0,0.06);">
<div style="font-size:1rem; font-weight:700; color:#1e293b; margin-bottom:0.35rem;" id="http-coming-title">HTTP/2 benchmarks</div>
<div style="font-size:0.85rem; color:#64748b; line-height:1.6;">Not yet available. We plan to add <span id="http-coming-ver">HTTP/2</span> benchmarking support in a future update. Want to help? <a href="https://github.com/MDA2AV/HttpArena" style="color:#3b82f6; font-weight:600; text-decoration:none;">Contribute on GitHub</a>.</div>
</div>
<script>
(function() {
  var tabs = document.querySelectorAll('.http-ver');
  var card = document.getElementById('http-coming-soon');
  var leaderboard = document.querySelector('.lb-card');
  var labels = { h2: 'HTTP/2', h3: 'HTTP/3' };
  tabs.forEach(function(tab) {
    tab.addEventListener('click', function() {
      tabs.forEach(function(t) {
        t.style.background = 'rgba(0,0,0,0.03)';
        t.style.color = '#94a3b8';
        t.style.borderColor = '#e2e8f0';
        t.classList.remove('active');
      });
      tab.style.background = 'rgba(59,130,246,0.12)';
      tab.style.color = '#2563eb';
      tab.style.borderColor = '#3b82f6';
      tab.classList.add('active');
      var ver = tab.dataset.ver;
      var wrapper = document.getElementById('lb-wrapper');
      if (ver === 'h1') {
        card.style.display = 'none';
        wrapper.style.display = '';
      } else {
        card.style.display = 'block';
        document.getElementById('http-coming-title').textContent = labels[ver] + ' benchmarks';
        document.getElementById('http-coming-ver').textContent = labels[ver];
        wrapper.style.display = 'none';
      }
    });
  });
})();
</script>
</div>

<div id="lb-wrapper">
{{< leaderboard >}}
</div>
