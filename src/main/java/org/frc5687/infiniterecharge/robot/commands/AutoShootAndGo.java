package org.frc5687.infiniterecharge.robot.commands;

import edu.wpi.first.wpilibj2.command.ParallelDeadlineGroup;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import org.frc5687.infiniterecharge.robot.Constants;
import org.frc5687.infiniterecharge.robot.OI;
import org.frc5687.infiniterecharge.robot.subsystems.*;
import org.frc5687.infiniterecharge.robot.util.Limelight;
import org.frc5687.infiniterecharge.robot.util.PoseTracker;

import java.lang.module.FindException;

public class AutoShootAndGo extends SequentialCommandGroup {

    public AutoShootAndGo(Turret turret, Shooter shooter, Hood hood, Limelight limelight, DriveTrain driveTrain, PoseTracker poseTracker, Indexer indexer) {
        addCommands(
            new ParallelDeadlineGroup(new AutoShoot(shooter, indexer, turret, null, 0), new AutoTarget(turret, shooter, hood, limelight, driveTrain, poseTracker, null, Constants.Hood.NEAR_TARGET_HOOD_ANGLE_DEGREES, Constants.Shooter.NEAR_TARGET_SHOOTER_SPEED_PERCENT)),
            new AutoDrive(driveTrain, -48)
        );
   }

}