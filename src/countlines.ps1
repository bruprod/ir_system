$total = 0
$files = Get-ChildItem -Recurse | Where { $_.Extension -eq '.java' } 

foreach($f in $files) {
  $lines = (Get-Content $f).count
  write-host "$lines - $f"
  $total += $lines
}

Write-Host "Total Lines: $total"