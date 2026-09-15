$src = 'C:\Users\xinan\AppData\Local\Temp\opencode\h2_dump.sql'
$dst = 'C:\Users\xinan\AppData\Local\Temp\opencode\mysql_load.sql'
Add-Type -TypeDefinition @'
using System;
using System.IO;
using System.Text;
using System.Text.RegularExpressions;

public static class Migrator
{
    public static void Run(string src, string dst)
    {
        string[] lines = File.ReadAllLines(src, Encoding.ASCII);
        var sb = new StringBuilder();
        sb.AppendLine("SET NAMES utf8mb4;");
        sb.AppendLine("SET SESSION sql_mode = 'NO_BACKSLASH_ESCAPES';");
        bool inInsert = false;
        foreach (string raw in lines)
        {
            string line = raw;
            string t = line.Trim();
            if (t.StartsWith("INSERT INTO \"PUBLIC\"."))
            {
                var m = Regex.Match(t, "^INSERT INTO \"PUBLIC\"\\.\"([A-Z_]+)\"(.*)$");
                if (m.Success)
                    line = "INSERT INTO " + m.Groups[1].Value.ToLowerInvariant() + m.Groups[2].Value;
                inInsert = true;
                sb.AppendLine(DecodeLine(line));
                if (t.EndsWith(";")) inInsert = false;
            }
            else if (inInsert)
            {
                sb.AppendLine(DecodeLine(line));
                if (t.EndsWith(";")) inInsert = false;
            }
        }
        File.WriteAllText(dst, sb.ToString(), new UTF8Encoding(false));
    }

    private static string DecodeLine(string line)
    {
        line = Regex.Replace(line, "TIMESTAMP '(?<v>\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}(\\.\\d+)?)'", "'${v}'");
        return Regex.Replace(line, "U&'((?:[^']|'')*)'", new MatchEvaluator(DecodeU));
    }

    private static string DecodeU(Match m)
    {
        string inner = m.Groups[1].Value.Replace("''", "'");
        var sb = new StringBuilder();
        for (int i = 0; i < inner.Length; i++)
        {
            char c = inner[i];
            if (c == '\\' && i + 1 < inner.Length)
            {
                if (inner[i + 1] == '\\') { sb.Append('\\'); i++; }
                else if (i + 4 < inner.Length && IsHex(inner.Substring(i + 1, 4)))
                {
                    sb.Append((char)Convert.ToInt32(inner.Substring(i + 1, 4), 16));
                    i += 4;
                }
                else { sb.Append('\\'); }
            }
            else sb.Append(c);
        }
        return "'" + sb.ToString().Replace("'", "''") + "'";
    }

    private static bool IsHex(string s)
    {
        foreach (char c in s)
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')))
                return false;
        return true;
    }
}
'@
[Migrator]::Run($src, $dst)
$out = Get-Item $dst
"WROTE $($out.FullName) size=$($out.Length)"