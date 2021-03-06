package org.frc5687.infiniterecharge.robot.subsystems;

import com.kauailabs.navx.frc.AHRS;
import com.revrobotics.AlternateEncoderType;
import com.revrobotics.CANEncoder;
import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel;
import edu.wpi.first.wpilibj.controller.PIDController;
import edu.wpi.first.wpilibj.controller.SimpleMotorFeedforward;

import edu.wpi.first.wpilibj.geometry.Pose2d;
import edu.wpi.first.wpilibj.geometry.Rotation2d;
import edu.wpi.first.wpilibj.kinematics.DifferentialDriveKinematics;
import edu.wpi.first.wpilibj.kinematics.DifferentialDriveOdometry;
import edu.wpi.first.wpilibj.kinematics.DifferentialDriveWheelSpeeds;
import edu.wpi.first.wpilibj.trajectory.TrajectoryConfig;
import edu.wpi.first.wpilibj.util.Units;
import org.frc5687.infiniterecharge.robot.Constants;
import org.frc5687.infiniterecharge.robot.OI;
import org.frc5687.infiniterecharge.robot.RobotMap;
import org.frc5687.infiniterecharge.robot.util.BasicPose;
import org.frc5687.infiniterecharge.robot.util.Limelight;
import org.frc5687.infiniterecharge.robot.util.OutliersContainer;

import static org.frc5687.infiniterecharge.robot.Constants.DriveTrain.*;
import static org.frc5687.infiniterecharge.robot.util.Helpers.applySensitivityFactor;
import static org.frc5687.infiniterecharge.robot.util.Helpers.limit;

public class DriveTrain extends OutliersSubsystem {
    private CANSparkMax _leftMaster;
    private CANSparkMax _leftSlave;
    private CANSparkMax _rightMaster;
    private CANSparkMax _rightSlave;

    private CANEncoder _leftEncoder;
    private CANEncoder _rightEncoder;

    private DifferentialDriveOdometry _odometry;
    private DifferentialDriveKinematics _driveKinematics;
    private SimpleMotorFeedforward _driveFeedForward;
    private TrajectoryConfig _driveConfig;

    private PIDController _angleController;
    private double _targetAngle;
    private double _previousRotation = 999;
    private boolean _anglePIDEnabled = false;

    private double _previousSpeed = 0;

    private OI _oi;
    private AHRS _imu;

    private Pose2d _pose;
    private Shifter _shifter;
    private Limelight _driveLimelight;

    private double _xLength;
    private double _yLength;

    private double _oldLeftSpeedFront;
    private double _oldLeftSpeedBack;
    private double _oldRightSpeedFront;
    private double _oldRightSpeedBack;
    private boolean _isPaused = false;
    private double _prevAngle;

    public DriveTrain(OutliersContainer container, OI oi, AHRS imu, Shifter shifter, Limelight driveLimelight)  {
        super(container);
        _oi = oi;
        _imu = imu;
        _shifter = shifter;
        _driveLimelight = driveLimelight;
        try {
            debug("Allocating motor controllers");
            _leftMaster = new CANSparkMax(RobotMap.CAN.SPARKMAX.LEFT_MASTER, CANSparkMaxLowLevel.MotorType.kBrushless);
            _rightMaster = new CANSparkMax(RobotMap.CAN.SPARKMAX.RIGHT_MASTER, CANSparkMaxLowLevel.MotorType.kBrushless);
            _leftSlave = new CANSparkMax(RobotMap.CAN.SPARKMAX.LEFT_FOLLOWER, CANSparkMaxLowLevel.MotorType.kBrushless);
            _rightSlave = new CANSparkMax(RobotMap.CAN.SPARKMAX.RIGHT_FOLLOWER, CANSparkMaxLowLevel.MotorType.kBrushless);


            _leftEncoder = _leftMaster.getAlternateEncoder(AlternateEncoderType.kQuadrature, Constants.DriveTrain.CPR);
            _rightEncoder = _rightMaster.getAlternateEncoder(AlternateEncoderType.kQuadrature, Constants.DriveTrain.CPR);

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
        _rightEncoder.setInverted(false);
        _leftEncoder.setInverted(true);
        _leftSlave.follow(_leftMaster);
        _rightSlave.follow(_rightMaster);
        resetDriveEncoders();

        _driveKinematics = new DifferentialDriveKinematics(Units.inchesToMeters(WIDTH));
        _odometry = new DifferentialDriveOdometry(getHeading(), new Pose2d(0,0, new Rotation2d(0)));
        _driveFeedForward = new SimpleMotorFeedforward(KS_VOLTS, KV_VOLTSPR, KA_VOLTSQPR);
        _driveConfig = new TrajectoryConfig(MAX_ACCEL_MPS, MAX_ACCEL_MPS).setKinematics(_driveKinematics);

        _angleController = new PIDController(Constants.DriveStraight.kP_ANGLE, Constants.DriveStraight.kI_ANGLE, Constants.DriveStraight.kD_ANGLE, Constants.UPDATE_PERIOD);
        _angleController.enableContinuousInput(-180, 180);
        _angleController.setTolerance(Constants.DriveStraight.ANGLE_TOLERANCE);
    }

    public void enableBrakeMode() {
        _leftMaster.setIdleMode(CANSparkMax.IdleMode.kBrake);
        _leftSlave.setIdleMode(CANSparkMax.IdleMode.kCoast);
        _rightMaster.setIdleMode(CANSparkMax.IdleMode.kBrake);
        _rightSlave.setIdleMode(CANSparkMax.IdleMode.kCoast);
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
        if (rotation == 0 && !_anglePIDEnabled) {
                // We've just started "driving straight"
                // Initialize and enable the angle controller
            _anglePIDEnabled = true;
            _targetAngle = _imu.getYaw();
            _angleController.setSetpoint(_targetAngle);
            _angleController.reset();
        } else if (rotation!=0 &&  _anglePIDEnabled || speed == 0) {
            _anglePIDEnabled = false;
        } else if (_anglePIDEnabled) {
            // Get rotation from the angle controller
            rotation = limit(_angleController.calculate(_imu.getYaw()), -.15, 0.15);
        }

        Shifter.Gear gear = _shifter.getGear();
        if (_shifter.getGear() == Shifter.Gear.HIGH) {
            speed = limit(speed, Constants.DriveTrain.SPEED_LIMIT);
        }
        rotation = limit(rotation, 1);

        double leftMotorOutput;
        double rightMotorOutput;

        double maxInput = Math.copySign(Math.max(Math.abs(speed), Math.abs(rotation)), speed);

        if (speed < Constants.DriveTrain.DEADBAND && speed > -Constants.DriveTrain.DEADBAND) {
            // Turning in place
//            _previousSpeed = speed;

            if (!_anglePIDEnabled) {
                if (!override) {
                    rotation = applySensitivityFactor(rotation, _shifter.getGear() == Shifter.Gear.HIGH ? Constants.DriveTrain.ROTATION_SENSITIVITY_HIGH_GEAR : Constants.DriveTrain.ROTATION_SENSITIVITY_LOW_GEAR);
                }
                if (creep) {
                    rotation = rotation * CREEP_FACTOR;
                } else {
                    rotation = rotation * 0.8;
                }
            }
            leftMotorOutput = rotation;
            rightMotorOutput = -rotation;
        } else {
            // Square the inputs (while preserving the sign) to increase fine control
            // while permitting full power.
            speed = Math.copySign(applySensitivityFactor(speed, Constants.DriveTrain.SPEED_SENSITIVITY), speed);
            if (!override && !_anglePIDEnabled) {
                rotation = applySensitivityFactor(rotation, _shifter.getGear() == Shifter.Gear.HIGH ? Constants.DriveTrain.TURNING_SENSITIVITY_HIGH_GEAR : Constants.DriveTrain.TURNING_SENSITIVITY_LOW_GEAR);
            }
            // rotation = applySensitivityFactor(rotation, Constants.DriveTrain.ROTATION_SENSITIVITY);
            double delta = (override || _anglePIDEnabled) ? rotation : rotation * Math.abs(speed);

            // If in low gear, apply ramping.

//            if (_shifter.getGear() == Shifter.Gear.LOW) {
//                if (_previousSpeed > 0) {
//                    error("prev speed greater than 0");
//                    speed = limit(speed, 0, _previousSpeed + Constants.DriveTrain.RAMP_INCREMENT_LOWGEAR);
//                } else if (_previousSpeed < 0 || speed < 0) {
//                    error("prev speed less than 0");
//                    speed = limit(speed, _previousSpeed - Constants.DriveTrain.RAMP_INCREMENT_LOWGEAR, 0);
//                } else {
//                    error("else increment");
//                    speed = limit(speed, Constants.DriveTrain.RAMP_INCREMENT_LOWGEAR, Constants.DriveTrain.RAMP_INCREMENT_LOWGEAR);
//                }
//            }
//            _previousSpeed = speed;

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
//            metric("Str/LeftMotor", leftMotorOutput);
//            metric("Str/RightMotor", rightMotorOutput);
        }

        setPower(limit(leftMotorOutput), limit(rightMotorOutput), true);
    }
    public void setPower(double leftSpeed, double rightSpeed, boolean override) {
        _leftMaster.set(leftSpeed);
        _rightMaster.set(rightSpeed);
        _leftSlave.set(leftSpeed);
        _rightSlave.set(rightSpeed);
//        metric("Power/Right", rightSpeed);
//        metric("Power/Left", leftSpeed);
    }
    public double getRawLeftEncoder() {
        return _leftEncoder.getPosition();
    }
    public double getRawRightEncoder() {
        return _rightEncoder.getPosition();
    }
    public double getLeftDistance() {
        return getRawLeftEncoder() * Constants.DriveTrain.ENCODER_CONVERSION;
    }
    public double getRightDistance() {
        return getRawRightEncoder() * Constants.DriveTrain.ENCODER_CONVERSION;
    }
    public double getDistance() {
        return (getLeftDistance() + getRightDistance()) / 2;
    }
    public double getLeftVelocity() {
        return _leftEncoder.getVelocity() * 2 * Math.PI * Units.inchesToMeters(2) / 60; //Meters Per Sec
    }
    public double getRightVelocity() {
        return _rightEncoder.getVelocity() * 2 * Math.PI * Units.inchesToMeters(2.0) / 60 ; //Meters Per Sec
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
//        updatePose();
        _pose = _odometry.update(getHeading(), Units.inchesToMeters(getLeftDistance()), Units.inchesToMeters(getRightDistance()));
        if (_driveLimelight.isTargetSighted() && _oi.isAutoTargetDrivePressed() && _driveLimelight.getTargetDistance() < Constants.DriveTrain.LIMELIGHT_ODOMETRY_ZONE) {
            resetOdometry(updatePose());
        }
    }

    @Override
    public void updateDashboard() {
//        metric("X", getPose().getTranslation().getX());
//        metric("Y", getPose().getTranslation().getY());
//        metric("leftDistane", getLeftDistance());
//        metric("rightDistance", getRightDistance());
        metric("angle to target", getAngleToTarget());
        metric("distance to taget", distanceToTarget());
        metric("using pid", _anglePIDEnabled);
        metric("heading", _imu.getYaw());
        metric("target angle", _targetAngle);

    }

    public DifferentialDriveKinematics getKinematics() {
        return _driveKinematics;
    }

    public Rotation2d getHeading() {
        return Rotation2d.fromDegrees(-_imu.getYaw());
    }

    public double getYaw() {return _imu.getYaw();}

    public Pose2d getPose() {
        return _odometry.getPoseMeters();
    }

    public DifferentialDriveWheelSpeeds getWheelSpeeds() { return new DifferentialDriveWheelSpeeds(getLeftVelocity(), getRightVelocity()); }

    public SimpleMotorFeedforward getDriveTrainFeedForward() {
        return _driveFeedForward;
    }

    public TrajectoryConfig getDriveConfig(boolean reversed) {
        return _driveConfig.setReversed(reversed);
    }

    public void resetOdometry(Pose2d pose) {
        resetDriveEncoders();
        _odometry.resetPosition(pose, getHeading());
    }

    public void resetDriveEncoders() {
        _leftEncoder.setPosition(0);
        _rightEncoder.setPosition(0);
    }

    public double distanceToTarget() {
        double x = _pose.getTranslation().getX();
        double y = _pose.getTranslation().getY();
        double targetX = Constants.AutoPositions.TARGET_POSE.getTranslation().getX();
        double targetY = Constants.AutoPositions.TARGET_POSE.getTranslation().getY();
        _xLength = targetX - x;
        _yLength = targetY - y;
        return Math.sqrt((_xLength * _xLength) + (_yLength * _yLength));
    }

    public double getAngleToTarget() {
        double angle = 0;
        metric("xLength", _xLength);
        metric("ylength", _yLength);
        if (_yLength > 0) {
            angle = (90 + Math.toDegrees(Math.asin(_xLength / distanceToTarget())) + getHeading().getDegrees());
            if (!Double.isNaN(angle)) {
                _prevAngle = angle;
            }
        } else if (_yLength < 0){
            angle = -(Math.toDegrees(Math.asin(_xLength / distanceToTarget())) + 90) + getHeading().getDegrees();
            if (!Double.isNaN(angle)) {
                _prevAngle = angle;
            }
        }
        if (Double.isNaN(angle)) {
            return _prevAngle;
        } else {
            return angle;
        }
    }

    public BasicPose getDrivePose() {
        return new BasicPose(_imu.getAngle(), _leftEncoder.getPosition(), _rightEncoder.getPosition(), 0);
    }
    public Pose2d updatePose() {
        Pose2d prevPose = getPose();
        double distance = Units.inchesToMeters(_driveLimelight.getTargetDistance());
        double alpha = 90 - Math.abs(_driveLimelight.getHorizontalAngle());
        double x = Math.sin(Math.toRadians(alpha)) * distance;
        double y = Math.cos(Math.toRadians(alpha)) * distance;
        double poseX = Constants.AutoPositions.LOADING_STATION_POSE.getTranslation().getX() - x;
        double poseY = 0;
        if (prevPose.getTranslation().getY() < Constants.AutoPositions.LOADING_STATION_POSE.getTranslation().getY()) {
            poseY = Constants.AutoPositions.LOADING_STATION_POSE.getTranslation().getY() - y;
        } else if (prevPose.getTranslation().getY() > Constants.AutoPositions.LOADING_STATION_POSE.getTranslation().getY()) {
            poseY = Constants.AutoPositions.LOADING_STATION_POSE.getTranslation().getY() + y;
        }
        return new Pose2d(poseX, poseY, getHeading());
    }
}
