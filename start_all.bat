@echo off
chcp 65001 >nul
echo ========================================
echo     嘉园外卖项目一键启动脚本
echo ========================================
echo.

echo [1/4] 启动 MySQL...
net start MySQL80
if %errorlevel% equ 0 (
    echo     ✓ MySQL 启动成功
) else (
    echo     ✗ MySQL 启动失败，请手动启动
    echo     请按 Win+R 输入 services.msc 启动 MySQL80 服务
)
echo.

echo [2/4] 启动 Redis...
if exist "D:\redis\redis-install-path\redis-server.exe" (
    start "Redis" /D "D:\redis\redis-install-path" redis-server.exe redis.windows.conf
    echo     ✓ Redis 启动中...
) else (
    echo     ✗ Redis 未找到，请确认安装路径
    echo     默认路径: D:\redis\redis-install-path\redis-server.exe
)
echo.

echo [3/4] 启动后端服务...
if exist "d:\small-third\project1\hanye-take-out-main\hanye-take-out-springboot3\server\target\hanye-take-out-server-1.0-SNAPSHOT.jar" (
    cd /d "d:\small-third\project1\hanye-take-out-main\hanye-take-out-springboot3\server"
    start "Backend" java -jar target/hanye-take-out-server-1.0-SNAPSHOT.jar
    echo     ✓ 后端服务启动中...
    echo     等待10秒让后端完全启动...
    timeout /t 10 /nobreak >nul
) else (
    echo     ✗ 后端 jar 包未找到，需要先编译
    echo     请执行: mvn clean package -DskipTests
)
echo.

echo [4/4] 启动前端服务...
if exist "d:\small-third\project1\hanye-take-out-main\hanye-take-out-vue3\node_modules" (
    cd /d "d:\small-third\project1\hanye-take-out-main\hanye-take-out-vue3"
    start "Frontend" npm run dev
    echo     ✓ 前端服务启动中...
) else (
    echo     ✗ 前端依赖未安装
    echo     请先执行: npm install --ignore-scripts
)
echo.

echo ========================================
echo     启动完成！
echo     前端地址: http://localhost:5173
echo     后端地址: http://localhost:8081
echo ========================================
pause