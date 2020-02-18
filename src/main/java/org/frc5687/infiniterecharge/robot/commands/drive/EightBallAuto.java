package org.frc5687.infiniterecharge.robot.commands.drive;

import com.kauailabs.navx.frc.AHRS;
import edu.wpi.first.wpilibj2.command.ParallelDeadlineGroup;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.WaitCommand;
import org.frc5687.infiniterecharge.robot.commands.*;
import org.frc5687.infiniterecharge.robot.subsystems.*;
import org.frc5687.infiniterecharge.robot.util.Limelight;
import org.frc5687.infiniterecharge.robot.util.PoseTracker;

public class EightBallAuto extends SequentialCommandGroup {
    public EightBallAuto(DriveTrain driveTrain, Turret turret, Shooter shooter, Hood hood, Intake intake, AHRS imu, Indexer indexer, Lights lights, Limelight limelight, PoseTracker poseTracker) {
        addCommands(
                new ParallelDeadlineGroup(
                        new AutoDrivePath(driveTrain, imu, "StartingToGenerator", 0,false),
                        new AutoIntake(intake, lights)
                )
//                , new AutoAlign(driveTrain, 35)
                ,  new AutoDrivePath(driveTrain, imu, "HalfTrench", 0, true)
                ,new ParallelDeadlineGroup(
                        new AutoShoot(shooter, indexer, turret, null)
                        ,new AutoTarget(turret, shooter, hood, limelight, driveTrain, poseTracker, lights,null,4300, 61) //TODO: Tune
                        ,  new AutoAlign(driveTrain, 0)
                )
                ,  new ParallelDeadlineGroup(
                        new AutoDrivePath(driveTrain, imu, "TrenchBalls", 0, false),
                        new AutoIntake(intake, lights)
                )
                ,new ParallelDeadlineGroup(
                    new AutoShoot(shooter, indexer, turret, null),
                        new AutoAlign(driveTrain, 0),
                        new AutoTarget(turret, shooter, hood, limelight, driveTrain, poseTracker, lights,null,4500, 63) //TODO: Tune
                )
        );
    }
}