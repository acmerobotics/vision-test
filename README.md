# vision-test

Gradle-based Eclipse project for testing the [vision-lib](https://github.com/acmerobotics/vision-lib) repo. 

## Installation

* Clone the repo with `git clone https://github.com/acmerobotics/vision-test --depth=1`. 
* Install the vision library submodule using `git submodule update --init`.
* Run `./gradlew eclipse` inside the directory to generate the necessary project files.
* If you haven't already, open Eclipse.
* Under the File menu, select Import.... In the Import wizard, select Existing Projects into Workspace from under the General tab and click Next. 
* Click the Browse... button next to Select root directory and select the workspace from the filesystem.
* In the Projects area below, make sure visionTest and visionLib are selected and then click Finish.
* Open `Main.java` in the visionTest project and run it. You'll notice that it failed because we haven't added the proper command line arguments. To do this, select Run > Run Configurations..., select Main from the left panel, select Arguments from the list of tabs on the right, and paste `--input=images/* --output=output/` into the Program arguments. When you're finished, click Run at the bottom. 
* Now the application should run fine. By default, it takes the images in `visionTest\images` and process them, putting the results in `visionTest\output`.