using System;
using System.Diagnostics;
using System.IO;
using System.Windows.Forms;

class Launcher
{
    [STAThread]
    static void Main()
    {
        try
        {
            string root = AppDomain.CurrentDomain.BaseDirectory;

            // Load environment variables from .env file if it exists
            string envPath = Path.Combine(root, ".env");
            if (File.Exists(envPath))
            {
                foreach (string line in File.ReadAllLines(envPath))
                {
                    string trimmed = line.Trim();
                    if (string.IsNullOrEmpty(trimmed) || trimmed.StartsWith("#")) continue;
                    int eq = trimmed.IndexOf('=');
                    if (eq > 0)
                    {
                        string key = trimmed.Substring(0, eq).Trim();
                        string val = trimmed.Substring(eq + 1).Trim();
                        Environment.SetEnvironmentVariable(key, val);
                    }
                }
            }

            // Determine java executable path (prefer windowed javaw.exe to avoid console)
            string javawPath = Path.Combine(root, @"dist\Biblio-Java-Windows-x64\runtime\bin\javaw.exe");
            if (!File.Exists(javawPath))
            {
                javawPath = Path.Combine(root, @"dist\jpackage\Biblio-Java-Windows-x64\runtime\bin\javaw.exe");
            }
            if (!File.Exists(javawPath))
            {
                javawPath = "javaw.exe";
            }

            ProcessStartInfo psi = new ProcessStartInfo();
            psi.FileName = javawPath;
            psi.Arguments = "-cp \"out;lib\\postgresql-42.7.4.jar;lib\\flatlaf-3.5.4.jar\" GUI_Main";
            psi.WorkingDirectory = root;
            psi.UseShellExecute = false;
            psi.CreateNoWindow = true;
            psi.WindowStyle = ProcessWindowStyle.Hidden;

            Process.Start(psi);
        }
        catch (Exception ex)
        {
            MessageBox.Show("Erreur lors du lancement de l'application: " + ex.Message, "Biblio-Java", MessageBoxButtons.OK, MessageBoxIcon.Error);
        }
    }
}
