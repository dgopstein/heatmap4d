# http://unix.stackexchange.com/questions/120529/compare-files-and-tell-how-similar-they-are
{if (NR==FNR) s[++m]=$0; else t[++n]=$0}
  function min(x, y) {
    return x < y ? x : y
  }
  END {
    for(i=0;i<=m;i++) d[i,0] = i
    for(j=0;j<=n;j++) d[0,j] = j

    for(i=1;i<=m;i++) {
      for(j=1;j<=n;j++) {
        c = s[i] != t[j]
        d[i,j] = min(d[i-1,j]+1,min(d[i,j-1]+1,d[i-1,j-1]+c))
      }
    }
    print d[m,n]
  }
