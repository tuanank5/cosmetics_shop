// These paths belong to HttpLoi_MariaDb_Test.TraceConfig, never to the release router.
module.exports=[400,409,413,429,500,503].map(code=>({
  route:code===503?'error fallback':'error '+code,template:code===503?'error':'error/'+code,
  example:'/products/sec01-status-'+code,roles:['Guest','Customer','Admin'],status:code,state:'error'
}));
