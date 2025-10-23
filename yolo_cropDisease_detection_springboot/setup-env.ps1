<#
检查并打印开发环境建议：Java（建议 JDK 1.8 或 11）、Maven Wrapper，可选的 setx 命令示例。
不要自动修改系统变量，仅提供建议和可复制命令。
#>
Write-Host "== 环境检查：开始 =="
# Java
Write-Host "-- Java 信息 --"
try {
    & java -version 2>&1 | ForEach-Object { Write-Host $_ }
} catch {
    Write-Host "未找到 java 命令。请安装 JDK (建议 1.8 或 11)。"
}
Write-Host "JAVA_HOME: $env:JAVA_HOME"

# Maven Wrapper
$mvnw = Join-Path -Path (Get-Location) -ChildPath 'mvnw.cmd'
if (Test-Path $mvnw) {
    Write-Host "-- Maven Wrapper (mvnw.cmd) 可用 --"
    try {
        & $mvnw -v 2>&1 | ForEach-Object { Write-Host $_ }
    } catch {
        Write-Host "无法运行 mvnw.cmd，请确保 JAVA_HOME 配置正确并重试。"
    }
} else {
    Write-Host "未在项目根找到 mvnw.cmd。可使用本地 mvn 或检查 .mvn/wrapper。"
}

Write-Host "\n建议："
Write-Host "1) 如果你需要严格与项目兼容，安装 JDK 1.8（Temurin/Adoptium 或 Oracle）。"
Write-Host "   推荐下载页: https://adoptium.net/zh/"
Write-Host "2) 安装后在 PowerShell 中执行（示例，假设 JDK 安装在 C:\\Java\\jdk1.8.0_xxx）："
Write-Host "   setx -m JAVA_HOME 'C:\\Java\\jdk1.8.0_xxx'"
Write-Host "   $env:JAVA_HOME = 'C:\\Java\\jdk1.8.0_xxx'  # 临时生效当前会话"
Write-Host "   [Environment]::SetEnvironmentVariable('Path', $env:Path + ';' + $env:JAVA_HOME + '\\bin', 'Machine')  # 可选：为所有用户追加 PATH"
Write-Host "3) 使用 Maven Wrapper 编译项目：在项目根运行 `./mvnw.cmd -DskipTests package`（Windows PowerShell）"
Write-Host "\n== 环境检查：结束 =="

Write-Host "\n附注：当前脚本假定在 Java 17 环境下也可以构建（项目已通过 jdk9-plus profile 兼容 Lombok）。"
Write-Host "如果你使用 JDK >= 9，则 Maven 会自动激活名为 'jdk9-plus' 的 profile 以启用必要的编译参数。"