
import tkinter as tk
from tkinter import ttk
from tkinter import font as tkfont
from tkinter import *
from tkinter.ttk import *
import time, os, sys, subprocess


class CalciumSignalWizard(tk.Tk):
    def __init__(self, *args, **kwargs):

            tk.Tk.__init__(self, *args, **kwargs)

            self.title("CalciumSignal Installer")

            # the geometry function seems to be measured differently on unix
            if sys.platform == "win32" or sys.platform == "cygwin":
                self.geometry("390x140")
            else:
                self.geometry("430x140")

            self.eval('tk::PlaceWindow %s center' % self.winfo_toplevel())

            container = tk.Frame(self)
            container.pack(side = "top", fill = "both", expand = True)

            container.grid_rowconfigure(0, weight = 1)
            container.grid_columnconfigure(0, weight = 1)

            self.frames = {}

            for F in (StartPage, PageOne):
                frame = F(container, self)
                self.frames[F] = frame
                frame.grid(row = 0, column = 0, sticky ="nsew")

            self.show_frame(StartPage)

    def show_frame(self, cont):
        frame = self.frames[cont]
        frame.tkraise()
        frame.progress_frame()


class StartPage(tk.Frame):
    def __init__(self, parent, controller):
        tk.Frame.__init__(self, parent)

        label = ttk.Label(self, text ="This installer will set up the \nCalciumSignal plugin for "
                                       + "ImageJ. Click \"Next\" \nto begin.")

        label.grid(row = 0, column = 4, padx = 10, pady = 10)

        button1 = ttk.Button(self, text ="Next",
        command = lambda : controller.show_frame(PageOne))

        button1.grid(row = 10, column = 10, padx = 10, pady = 10)

    def progress_frame(self):
        pass


class PageOne(tk.Frame):
    def __init__(self, parent, controller):
        tk.Frame.__init__(self, parent)
        self.controller = controller
        self.label = tk.Label(self, text="Beginning installation...", width=50, height=2)
        self.label.grid(row = 0, column = 4, padx = 10, pady = 10)
        self.progress = Progressbar(self, orient = HORIZONTAL, length = 100, mode = 'indeterminate')
        self.progress.grid(row = 4, column = 4, padx = 10, pady = 10)
        button1 = tk.Button(self, text="Done",
                           command=lambda: sys.exit())
        button1.grid(row = 10, column = 4, padx = 10, pady = 10)


    def progress_frame(self):
        # move into whatever directory we're running this script from
        # everything will be relative to this script, which is
        # in release which is hopefully in the Fiji.app directory
        current_file = os.path.realpath(__file__)
        os.chdir(os.path.dirname(current_file))
        self.update()
        time.sleep(0.5)

        self.label.configure(text="Creating plugins/CalciumSignal directory...")
        self.update_idletasks()

        base = os.path.join("..", "plugins", "CalciumSignal")

        try:
            os.mkdir(base)
        except FileExistsError:
            pass

        time.sleep(0.5)
        self.progress['value'] = 25
        self.update()

        self.label.configure(text="Installing jar file...")
        self.update_idletasks()

        try:
            os.replace("CalciumSignal_.jar", os.path.join("..", "plugins", "CalciumSignal_.jar"))
        except FileNotFoundError:
            self.label.configure(text="Could not find CalciumSignal_.jar.\nBe sure that it is inside the release folder and try again.")
            self.update_idletasks()
            return

        time.sleep(0.5)
        self.progress['value'] = 50
        self.update()

        self.label.configure(text="Setting up Python environment...")
        self.update_idletasks()

        try:
            subprocess.check_call([sys.executable, '-m', 'pip', 'install', '-r' 'requirements.txt'])
        except subprocess.CalledProcessError:
            self.label.configure(text="Installation Failed. Show console output to a developer\n"
                                   + "and/or ensure you have installed the all listed requirements.")
            self.update_idletasks()
            return

        pythonscript = os.path.join(base, "pythonscript")

        try:
            os.mkdir(pythonscript)
            celldata = os.path.join(pythonscript, "cell_data")
            os.mkdir(celldata)
        except FileExistsError:
            pass

        try:
            os.replace("peakscript.py", os.path.join(pythonscript, "peakscript.py"))
        except FileNotFoundError:
            self.label.configure(text="Could not find peakscript.py.\nBe sure that it is inside the release folder and try again.")
            self.update_idletasks()
            return

        time.sleep(0.5)
        self.progress['value'] = 75
        self.update()

        self.label.configure(text="Setting up additional directories...")
        self.update_idletasks()
        edgedata = os.path.join(base, "edge_data")

        try:
            os.mkdir(edgedata)
        except FileExistsError:
            pass

        time.sleep(0.5)
        self.progress['value'] = 100
        self.update_idletasks()

        self.label.configure(text="Installation complete! After closing this installer,\nfeel free to delete the release directory.")
        self.update()


if __name__ == "__main__":
    app = CalciumSignalWizard()
    app.mainloop()