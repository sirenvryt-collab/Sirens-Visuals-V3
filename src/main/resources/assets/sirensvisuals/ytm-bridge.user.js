// ==UserScript==
// @name         Sirens Visuals - YouTube Music Bridge
// @namespace    sirensvisuals
// @version      2.0
// @description  Sends the current song to the Sirens Visuals Minecraft mod, mutes ads, and lets the game control playback.
// @match        https://music.youtube.com/*
// @match        https://www.youtube.com/*
// @grant        GM_xmlhttpRequest
// @connect      127.0.0.1
// @run-at       document-idle
// ==/UserScript==
(function () {
  'use strict';
  const URL = 'http://127.0.0.1:__PORT__/';
  const TOKEN = '__TOKEN__';
  let muteAds = true, mutedByUs = false;

  const q = (s) => document.querySelector(s);
  const txt = (s) => { const e = q(s); return e ? e.textContent.trim() : ''; };
  const video = () => q('video');

  function isAd() {
    const p = q('#movie_player');
    if (p && p.classList.contains('ad-showing')) return true;
    if (q('.ytp-ad-player-overlay, .ytp-ad-text, .ad-showing')) return true;
    return !!q('ytmusic-player-bar[is-ad], ytmusic-player-bar .advertisement');
  }

  function trySkip() {
    const b = q('.ytp-ad-skip-button, .ytp-skip-ad-button, .ytp-ad-skip-button-modern');
    if (b) b.click();
  }

  function info() {
    const ms = navigator.mediaSession && navigator.mediaSession.metadata;
    let title = ms ? ms.title : '', artist = ms ? ms.artist : '';
    if (!title) title = txt('ytmusic-player-bar .title') || txt('h1.ytd-watch-metadata yt-formatted-string');
    if (!artist) artist = (txt('ytmusic-player-bar .byline') || txt('#owner #channel-name a')).split('•')[0].trim();
    const v = video();
    const id = new URLSearchParams(location.search).get('v') || '';
    return {
      id, title, artist,
      duration: v && isFinite(v.duration) ? v.duration : 0,
      position: v ? v.currentTime : 0,
      playing: v ? !v.paused : false,
      ad: isAd(),
      source: location.hostname
    };
  }

  function run(cmd) {
    const v = video();
    if (cmd === 'play' && v) v.play();
    else if (cmd === 'pause' && v) v.pause();
    else if (cmd === 'next') (q('.next-button') || q('.ytp-next-button') || {click() {}}).click();
    else if (cmd === 'prev') (q('.previous-button') || q('.ytp-prev-button') || {click() {}}).click();
  }

  function tick() {
    const s = info();
    const v = video();
    if (v) {
      if (s.ad && muteAds) { v.muted = true; mutedByUs = true; trySkip(); }
      else if (mutedByUs) { v.muted = false; mutedByUs = false; }
    }
    GM_xmlhttpRequest({
      method: 'POST', url: URL, data: JSON.stringify(s), timeout: 1500,
      headers: { 'Content-Type': 'application/json', 'x-ds-token': TOKEN },
      onload: (r) => {
        try {
          const j = JSON.parse(r.responseText);
          muteAds = j.muteAds !== false;
          (j.cmds || []).forEach(run);
        } catch (e) {}
      },
      onerror: () => {}, ontimeout: () => {}
    });
  }
  setInterval(tick, 1000);
})();
