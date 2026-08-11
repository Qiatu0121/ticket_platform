-- 抢票：Redis 原子预扣（判断 + 扣减在一个脚本内完成，天然防超卖）
-- KEYS[1] = ticket:stock:{ticketId}
-- 返回 1 = 扣减成功；0 = 库存不足

local stock = redis.call('get', KEYS[1])

if not stock then
    -- Redis 里没有初始化库存，直接拒绝（正常情况下发布票种时会 initStock）
    return 0
end

if tonumber(stock) > 0 then
    redis.call('decr', KEYS[1])
    return 1
end

return 0
