using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;

var output = args.Length > 0 ? args[0] : Path.Combine(Environment.CurrentDirectory, "app.ico");
Directory.CreateDirectory(Path.GetDirectoryName(Path.GetFullPath(output))!);
using var bitmap = new Bitmap(256, 256, PixelFormat.Format32bppArgb);
using (var graphics = Graphics.FromImage(bitmap))
{
    graphics.SmoothingMode = SmoothingMode.AntiAlias;
    graphics.Clear(Color.Transparent);
    using var background = new SolidBrush(Color.FromArgb(91, 95, 239));
    using var shadow = new SolidBrush(Color.FromArgb(30, 17, 24, 39));
    graphics.FillEllipse(shadow, 18, 22, 220, 220);
    graphics.FillEllipse(background, 14, 14, 220, 220);
    using var font = new Font("Segoe UI", 78, FontStyle.Bold, GraphicsUnit.Pixel);
    using var foreground = new SolidBrush(Color.White);
    using var format = new StringFormat { Alignment = StringAlignment.Center, LineAlignment = StringAlignment.Center };
    graphics.DrawString("CF", font, foreground, new RectangleF(14, 8, 220, 220), format);
}

using var png = new MemoryStream();
bitmap.Save(png, ImageFormat.Png);
var bytes = png.ToArray();
using var stream = File.Create(output);
using var writer = new BinaryWriter(stream);
writer.Write((ushort)0);
writer.Write((ushort)1);
writer.Write((ushort)1);
writer.Write((byte)0);
writer.Write((byte)0);
writer.Write((byte)0);
writer.Write((byte)0);
writer.Write((ushort)1);
writer.Write((ushort)32);
writer.Write(bytes.Length);
writer.Write(22);
writer.Write(bytes);
Console.WriteLine(output);
