# CurseForge-Maven-Helper
A small program to work with the curseforge maven, figuring out dependencies and gradle links

## How to use
1. Get the URL of the file you want to use. The file should be in the format of https://minecraft.curseforge.com/projects/examplemod/files/12345
2. If you want to have the output be automatically written to your build.gradle, please put the file location in the `Gradle File` input box. (A "browse" box is in the works)
3. If you want Optional files to be asked, selct the "Include Option Libraries" box
4. Press the "Go" button, or press enter
5. If you've selected the "Include Option Libraries" and the project being processed has Option Libraries, A gui will come up asking you to select which ones to enter. Select the ones you desire, then either press the "Continue" button, press enter or close the GUI

##How to set up in workspace
1. Download zip file
2. Run `gradlew eclipse` or `gradlew idea`
3. In eclipse, there can be issues with security. This can be solved [here](https://stackoverflow.com/a/32062263)

##Contributing
If you want to contribute, go ahead. The only requirement I have is that the code has Same-line braces. (egyptian style)
