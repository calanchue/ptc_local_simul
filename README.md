# ptc_local_simul
Particle filer localization + 2d robot simulator, written in slick 2d java. 

## component 
- Robot simulater : gather sensor information.
- Remote simulater : Particle filter simulater. this will process sensoro information from the robot to localize the location of the robot in map
## how to run
1. run RobotSimul.java to launch Robot simulater. 
2. run RemoteSimul.java to launch Remote simulater. 
3. pressing '0' key to load map in both Simulater

## how to use
### robot simulater
![alt tag](https://github.com/calanchue/ptc_local_simul/blob/master/readme/robot_simul_2.PNG)

control

- arrow key : move, rotation
- space key : gather sensor information

legend

- big blue circle : actual robot location
- small blue circle : sensor infromation gathered
- red line : sensor range

###remote simulater
![alt tag](https://github.com/calanchue/ptc_local_simul/blob/master/readme/ptc_simul_2.PNG)

legend

- big blue circle : actual robot location
- small blue circle : sensor infromation gathered
- red line : sensor range
- magenta : mean of probable locatiaon
- light gray : probable robot location
- darker gary : sensor information which is gathered from the probable robot location
