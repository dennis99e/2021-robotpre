package frc.robot.subsytems;

import com.kauailabs.navx.frc.AHRS;
import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.SpeedControllerGroup;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.geometry.Pose2d;
import edu.wpi.first.wpilibj.geometry.Rotation2d;
import edu.wpi.first.wpilibj.kinematics.DifferentialDriveKinematics;
import edu.wpi.first.wpilibj.kinematics.DifferentialDriveOdometry;
import edu.wpi.first.wpilibj.kinematics.DifferentialDriveWheelSpeeds;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.Constants;
import frc.robot.OI;
import frc.robot.Robot;
import frc.robot.RobotMap;
import frc.robot.commands.Drive;

import static frc.robot.Constants.DriveTrain.CREEP_FACTOR;
import static frc.robot.util.Helpers.applySensitivityFactor;
import static frc.robot.util.Helpers.limit;

public class DriveTrain extends OutliersSubsystem {
    private CANSparkMax _leftMaster;
    private CANSparkMax _leftSlave;
    private CANSparkMax _rightMaster;
    private CANSparkMax _rightSlave;


    private DifferentialDriveOdometry _odometry;
    private DifferentialDriveKinematics _driveKinematics;

    private Encoder _leftMagEncoder;
    private Encoder _rightMagEncoder;

    private OI _oi;
    private AHRS _imu;
    private Robot _robot;
    private Shifter _shifter;

    private double _oldLeftSpeedFront;
    private double _oldLeftSpeedBack;
    private double _oldRightSpeedFront;
    private double _oldRightSpeedBack;
    private boolean _isPaused = false;

    public DriveTrain(Robot robot) {
        _oi = robot.getOI();
        _imu = robot.getIMU();
        _shifter = robot.getShifter();
        _robot = robot;

        try {
            debug("Allocating motor controllers");
            _leftMaster = new CANSparkMax(RobotMap.CAN.SPARKMAX.LEFT_MASTER, CANSparkMaxLowLevel.MotorType.kBrushless);
            _leftSlave = new CANSparkMax(RobotMap.CAN.SPARKMAX.LEFT_SLAVE, CANSparkMaxLowLevel.MotorType.kBrushless);
            _rightMaster = new CANSparkMax(RobotMap.CAN.SPARKMAX.RIGHT_MASTER, CANSparkMaxLowLevel.MotorType.kBrushless);
            _rightSlave = new CANSparkMax(RobotMap.CAN.SPARKMAX.RIGHT_SLAVE, CANSparkMaxLowLevel.MotorType.kBrushless);

            _leftMaster.restoreFactoryDefaults();
            _leftSlave.restoreFactoryDefaults();
            _rightMaster.restoreFactoryDefaults();
            _rightSlave.restoreFactoryDefaults();


            _leftMaster.setOpenLoopRampRate(Constants.DriveTrain.RAMP_RATE);
            _leftSlave.setOpenLoopRampRate(Constants.DriveTrain.RAMP_RATE);
            _rightMaster.setOpenLoopRampRate(Constants.DriveTrain.RAMP_RATE);
            _rightSlave.setOpenLoopRampRate(Constants.DriveTrain.RAMP_RATE);

            _leftMaster.setClosedLoopRampRate(Constants.DriveTrain.RAMP_RATE);
            _leftSlave.setClosedLoopRampRate(Constants.DriveTrain.RAMP_RATE);
            _rightMaster.setClosedLoopRampRate(Constants.DriveTrain.RAMP_RATE);
            _rightSlave.setClosedLoopRampRate(Constants.DriveTrain.RAMP_RATE);

            _leftMaster.setSmartCurrentLimit(Constants.DriveTrain.STALL_CURRENT_LIMIT, Constants.DriveTrain.FREE_CURRENT_LIMIT);
            _leftSlave.setSmartCurrentLimit(Constants.DriveTrain.STALL_CURRENT_LIMIT, Constants.DriveTrain.FREE_CURRENT_LIMIT);
            _rightMaster.setSmartCurrentLimit(Constants.DriveTrain.STALL_CURRENT_LIMIT, Constants.DriveTrain.FREE_CURRENT_LIMIT);
            _rightSlave.setSmartCurrentLimit(Constants.DriveTrain.STALL_CURRENT_LIMIT, Constants.DriveTrain.FREE_CURRENT_LIMIT);

            _leftMaster.setSecondaryCurrentLimit(Constants.DriveTrain.SECONDARY_LIMIT);
            _leftSlave.setSecondaryCurrentLimit(Constants.DriveTrain.SECONDARY_LIMIT);
            _rightMaster.setSecondaryCurrentLimit(Constants.DriveTrain.SECONDARY_LIMIT);
            _rightSlave.setSecondaryCurrentLimit(Constants.DriveTrain.SECONDARY_LIMIT);

            _leftMaster.setInverted(Constants.DriveTrain.LEFT_MOTORS_INVERTED);
            _leftSlave.setInverted(Constants.DriveTrain.LEFT_MOTORS_INVERTED);
            _rightMaster.setInverted(Constants.DriveTrain.RIGHT_MOTORS_INVERTED);
            _rightSlave.setInverted(Constants.DriveTrain.RIGHT_MOTORS_INVERTED);


        } catch (Exception e) {
            error("Exception allocating drive motor controllers: " + e.getMessage());
        }

        debug("Configuring mag encoders");
        _leftMagEncoder = new Encoder(RobotMap.DIO.DRIVE_LEFT_A, RobotMap.DIO.DRIVE_LEFT_B, false);
        _rightMagEncoder = new Encoder(RobotMap.DIO.DRIVE_RIGHT_A, RobotMap.DIO.DRIVE_RIGHT_B, false);
        _leftMagEncoder.setDistancePerPulse(Constants.DriveTrain.LEFT_DISTANCE_PER_PULSE);
        _rightMagEncoder.setDistancePerPulse(Constants.DriveTrain.RIGHT_DISTANCE_PER_PULSE);
        resetDriveEncoders();

        _driveKinematics = new DifferentialDriveKinematics(Constants.DriveTrain.WIDTH);
        _odometry = new DifferentialDriveOdometry(Rotation2d.fromDegrees(getYaw()));

        logMetrics("X", "Y", "Heading");
    }

    public void enableBrakeMode() {
        _leftMaster.setIdleMode(CANSparkMax.IdleMode.kBrake);
        _leftSlave.setIdleMode(CANSparkMax.IdleMode.kBrake);
        _rightMaster.setIdleMode(CANSparkMax.IdleMode.kBrake);
        _rightSlave.setIdleMode(CANSparkMax.IdleMode.kBrake);
    }

    public void disableBrakeMode() {
        _leftMaster.setIdleMode(CANSparkMax.IdleMode.kCoast);
        _leftSlave.setIdleMode(CANSparkMax.IdleMode.kCoast);
        _rightMaster.setIdleMode(CANSparkMax.IdleMode.kCoast);
        _rightSlave.setIdleMode(CANSparkMax.IdleMode.kCoast);
    }
    public void cheesyDrive(double speed, double rotation, boolean creep, boolean override) {
        metric("Speed", speed);
        metric("Rotation", rotation);

        speed = limit(speed, 1);
        //Shifter.Gear gear = _robot.getShifter().getGear();

        rotation = limit(rotation, 1);

        double leftMotorOutput;
        double rightMotorOutput;

        double maxInput = Math.copySign(Math.max(Math.abs(speed), Math.abs(rotation)), speed);

        if (speed < Constants.DriveTrain.DEADBAND && speed > -Constants.DriveTrain.DEADBAND) {
            if (!override) {
                rotation = applySensitivityFactor(rotation, _shifter.getGear() == Shifter.Gear.HIGH ? Constants.DriveTrain.ROTATION_SENSITIVITY_HIGH_GEAR : Constants.DriveTrain.ROTATION_SENSITIVITY_LOW_GEAR);
            }
            if (creep) {
                //metric("Rot/Creep", creep);
                rotation = rotation * CREEP_FACTOR;
            } else {
                rotation = rotation * 0.8;
            }

//            metric("Rot/Transformed", rotation);
            leftMotorOutput = rotation;
            rightMotorOutput = -rotation;
//            metric("Rot/LeftMotor", leftMotorOutput);
//            metric("Rot/RightMotor", rightMotorOutput);
        } else {
            // Square the inputs (while preserving the sign) to increase fine control
            // while permitting full power.
            metric("Str/Raw", speed);
            speed = Math.copySign(applySensitivityFactor(speed, Constants.DriveTrain.SPEED_SENSITIVITY), speed);
            if (!override) {
                rotation = applySensitivityFactor(rotation, _shifter.getGear() == Shifter.Gear.HIGH ? Constants.DriveTrain.TURNING_SENSITIVITY_HIGH_GEAR : Constants.DriveTrain.TURNING_SENSITIVITY_LOW_GEAR);
            }
            metric("Str/Trans", speed);
            rotation = applySensitivityFactor(rotation, Constants.DriveTrain.ROTATION_SENSITIVITY);
            double delta = override ? rotation : rotation * Math.abs(speed);
            if (override) {
                // speed = Math.copySign(limit(Math.abs(speed), 1-Math.abs(delta)), speed);

                if (speed + Math.abs(delta) > 1) {
                    speed = 1 - Math.abs(delta);
                }

                if (speed - Math.abs(delta) < -1) {
                    speed = -1 + Math.abs(delta);
                }
            }
            leftMotorOutput = speed + delta;
            rightMotorOutput = speed - delta;
            metric("Str/LeftMotor", leftMotorOutput);
            metric("Str/RightMotor", rightMotorOutput);
        }

        setPower(limit(leftMotorOutput), limit(rightMotorOutput), true);
    }
    public void setPower(double leftSpeed, double rightSpeed, boolean override) {
        _leftMaster.set(leftSpeed);
        _leftSlave.set (leftSpeed);
        _rightMaster.set(rightSpeed);
        _rightSlave.set (rightSpeed);
        metric("Power/Right", rightSpeed);
        metric("Power/Left", leftSpeed);
    }

    public void pauseMotors() {
        _oldLeftSpeedFront = _leftMaster.get();
        _oldLeftSpeedBack = _leftSlave.get();
        _oldRightSpeedFront = _rightMaster.get();
        _oldRightSpeedBack = _rightSlave.get();
        _leftSlave.set(0);
        _leftMaster.set(0);
        _rightMaster.set(0);
        _rightSlave.set(0);
        _isPaused = true;
    }

    public void resumeMotors() {
        _leftMaster.set(_oldLeftSpeedFront);
        _leftSlave.set(_oldLeftSpeedBack);
        _rightMaster.set(_oldRightSpeedFront);
        _rightSlave.set(_oldRightSpeedBack);
        _isPaused = false;
    }

    @Override
    public void periodic() {
        _odometry.update(Rotation2d.fromDegrees(getYaw()), _leftMagEncoder.getDistance()* 0.0254, _rightMagEncoder.getDistance()* 0.0254);
        setDefaultCommand(new Drive(this, _oi));
    }

    @Override
    public void updateDashboard() {
        SmartDashboard.putBoolean("MetricTracker/Drive", true);
        metric("X", getPose().getTranslation().getX());
        metric("Y", getPose().getTranslation().getY());
        metric("Heading", getPose().getRotation().getDegrees());
    }

    public DifferentialDriveKinematics getKinematics() {
        return _driveKinematics;
    }

    public float getYaw() {
        return _imu.getYaw();
    }
    public Pose2d getPose() {
        return _odometry.getPoseMeters();
    }

    public DifferentialDriveWheelSpeeds getWheelSpeeds() {
        return new DifferentialDriveWheelSpeeds(_leftMagEncoder.getRate(), _rightMagEncoder.getRate());
    }
    public void resetOdometry(Pose2d pose) {
        resetDriveEncoders();
        _odometry.resetPosition(pose, Rotation2d.fromDegrees(getYaw()));
    }

    public void tankDriveVolts(double leftVolts, double rightVolts) {
        _leftMaster.set(leftVolts);
        _leftSlave.set(leftVolts);
        _rightMaster.set(rightVolts);
        _rightSlave.set(rightVolts);
    }

    public void resetDriveEncoders() {
        _leftMagEncoder.reset();
        _rightMagEncoder.reset();
    }




}
