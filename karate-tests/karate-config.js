function fn() {
  let env = karate.env;
  karate.log("karate.env system property was:", env);
  if (!env) {
    env = 'local';
  }
  let config = {
    env: env,
    myVarName: 'someValue'
  }
  if (env === 'local') {
    config.rabbit_bridge = {
      url: 'http://localhost:8080'
    };
  } else if (env === 'dev') {
    config.rabbit_bridge = {
      url: 'https://if_you_see_this_set__bridge_domain:8080/bridge'
    };
  }
  return config;
}
