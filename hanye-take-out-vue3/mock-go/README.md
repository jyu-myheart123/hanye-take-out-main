# Go mock backend

这个目录是给当前前端项目准备的静态 Go 后端，接口路径直接兼容现有 Vite 代理：

- 前端请求 `/api/...`
- Vite 代理到 `http://localhost:8081/admin/...`
- 这个服务监听 `8081`

启动命令：

```powershell
$env:GOTELEMETRY='off'
cd .\mock-go
go run .
```

默认可用测试账号：

- `cyh / 123456`
- `linx / 123456`

说明：

- 数据主要放在内存里，重启后会恢复为初始静态数据。
- 已覆盖登录、员工、分类、菜品、套餐、订单、店铺状态、首页概览、统计图表、导出、WebSocket 占位连接。
