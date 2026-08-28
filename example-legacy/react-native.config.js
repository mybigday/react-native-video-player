const path = require('path');
const pkg = require('../package.json');

module.exports = {
  project: {
    ios: {
      automaticPodsInstallation: true,
    },
  },
  dependencies: {
    [pkg.name]: {
      root: path.join(__dirname, '..'),
      platforms: {
        // The codegen script incorrectly fails without this, so the platforms
        // are specified explicitly with an empty object.
        ios: {},
        android: {},
      },
    },
  },
};
