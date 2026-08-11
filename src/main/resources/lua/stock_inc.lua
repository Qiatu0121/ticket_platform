-- 回补库存：订单取消 / 抢票失败 / 超时关单时调用
-- KEYS[1] = ticket:stock:{ticketId}

redis.call('incr', KEYS[1])
return 1
