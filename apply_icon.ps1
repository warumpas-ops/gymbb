param(
    [Parameter(Mandatory=$true)]
    [string]$ImagePath
)

Add-Type -AssemblyName System.Drawing

$sizes = @{
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}

$baseDir = "C:\Users\Growth\.gemini\antigravity\scratch\gymbb\app\src\main\res"

$src = [System.Drawing.Image]::FromFile($ImagePath)

foreach ($entry in $sizes.GetEnumerator()) {
    $dir = Join-Path $baseDir $entry.Key
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force }
    $dest = Join-Path $dir "ic_launcher.png"
    
    $sz = $entry.Value
    $bmp = New-Object System.Drawing.Bitmap $sz, $sz
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    
    $g.DrawImage($src, 0, 0, $sz, $sz)
    $bmp.Save($dest, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $bmp.Dispose()
    Write-Host "Updated $dest ($sz x $sz)"
}

$src.Dispose()
Write-Host "All mipmap icons updated successfully!"
