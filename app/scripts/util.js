'use strict';

// http://stackoverflow.com/questions/105034/how-to-create-a-guid-uuid-in-javascript
var guid = (function() {
  function s4() {
    return Math.floor((1 + Math.random()) * 0x10000)
               .toString(16)
               .substring(1);
  }
  return function() {
    return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
           s4() + '-' + s4() + s4() + s4();
  };
})();


function capitalize(value) {
  if(!value || !value.length || value.length < 1) {
    return value;
  }
  return value.charAt(0).toUpperCase() + value.slice(1);
}
