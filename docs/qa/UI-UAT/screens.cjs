// A screen is one HTML view for a route family and audience. Shared create/edit forms
// are listed once. Error dispatch views are listed separately from business screens.
const screens = [];
function add(template, route, example, roles, state = 'default', status = 200) {
  screens.push({template, route, example, roles, state, status});
}
const all = ['Guest','Customer','Admin'], guest = ['Guest'], customer = ['Customer'], admin = ['Admin'];
add('home/index','/','/',all);
add('product/list','/products','/products',all);
add('product/detail','/products/{slug}','/products/fixture-a',all,'long');
add('cart/index','/cart','/cart',all);
add('chat/index','/chat','/chat',all);
add('order/momo-return','/payment/momo/return','/payment/momo/return',all);
for (const name of ['login','register','forgot-password','reset-password']) add('auth/'+name,'/'+name,'/'+name,guest);
for (const [view,route] of [['account','/account'],['security','/account/security'],['addresses','/account/addresses'],['wishlist','/account/wishlist']])
  add('auth/'+view,route,route,customer,view==='wishlist'?'empty':'default');
add('auth/address-form','/account/addresses/{new|id/edit}','/account/addresses/1/edit',customer);
add('order/checkout','/checkout','/checkout',customer);
add('order/list','/orders','/orders',customer);
add('order/customer-detail','/orders/{id}','/orders/701',customer);
add('order/review','/orders/items/{id}/review','/orders/items/701/review',customer);
add('return/form','/orders/{id}/returns/new','/orders/702/returns/new',customer);
add('return/list','/account/returns','/account/returns',customer);
add('return/detail','/account/returns/{id}','/account/returns/701',customer);
add('order/momo-mock','/payment/momo/mock/{id}',null,customer);
for (const [view,route,example] of [
 ['admin/dashboard','/admin/dashboard'],['admin/user/list','/admin/users'],['admin/user/detail','/admin/users/{id}','/admin/users/1'],
 ['admin/user/reviews','/admin/reviews'],['order/list','/admin/orders'],['order/detail','/admin/orders/{id}','/admin/orders/701'],
 ['return/list','/admin/returns'],['return/detail','/admin/returns/{id}','/admin/returns/701'],
 ['admin/chat/index','/admin/ai'],['admin/audit/list','/admin/audit'],['admin/audit/detail','/admin/audit/{id}','/admin/audit/701'],
 ['admin/voucher/list','/admin/vouchers'],['admin/voucher/form','/admin/vouchers/{new|id/edit}','/admin/vouchers/new'],
 ['admin/variant/list','/admin/catalog/products/{id}/variants','/admin/catalog/products/1/variants'],
 ['admin/variant/form','/admin/catalog/products/{id}/variants/{new|variantId/edit}','/admin/catalog/products/1/variants/1/edit']
]) add(view,route,example||route,admin);
for (const section of ['categories','brands','products']) {
  const prefix='/admin/catalog/'+section;
  add('admin/catalog/list',prefix,prefix,admin);
  add('admin/catalog/form',prefix+'/{new|id/edit}',prefix+'/1/edit',admin);
}
for (const code of [400,403,404,409,413,429,500]) {
  const example = code===404?'/products/absent-ui-fixture':code===403?'/admin/ai':null;
  add('error/'+code,'error '+code,example,code===403?customer:all,'error',code);
}
add('error','error fallback',null,all,'error');
module.exports=screens;
