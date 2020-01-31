package org.frc5687.infiniterecharge.robot;

public class RobotMap {

    /**
     * There should be an entry here for each CAN device, preferrably grouped by device type and then in numerical order.
     * Note that for CAN, ids must be unique per device type, but not across types.
     * Thus, you cannot have two SparkMax controllers with Id 0, but you can have a SparkMax with Id 0 and a TalonSRX with Id 0.
     */
    public static class CAN {

        public static class SPARKMAX {
            public static final int RIGHT_MASTER = 1;
            public static final int RIGHT_SLAVE = 2;
            public static final int LEFT_SLAVE = 3;
            public static final int LEFT_MASTER = 4;
            public static final int INTAKE_NEO = 6;
            public static final int SPINNER = 9;


        /*  Example:
                        public static final int LEFT_MASTER_SPARK= 1;
                        */
        }
        public static class TALONSRX {
            public static final int TURRET = 8;
        }
    }

        /**
         * There should be an entry here for each PWM port, preferrably in numerical order.
         * Note that for PWM only one device can connect to each port, so the numbers should be unique.
         */
        public static class PWM {
            /*  Example:
            public static final int ARM_VICTORSP = 0;
            */
            public static final int LEFT_FRONT_DRIVE_MOTOR = 0;
            public static final int LEFT_BACK_DRIVE_MOTOR = 1;
            public static final int RIGHT_FRONT_DRIVE_MOTOR = 2;
            public static final int RIGHT_BACK_DRIVE_MOTOR = 3;
        }

        /**
         * There should be an entry here for each PCM port, preferrably in numerical order.
         * Note that for PCM only one device can connect to each port, so the numbers should be unique.
         */
        public static class PCM {
            public static final int SHIFTER_HIGH = 5;
            public static final int SHIFTER_LOW = 4;
            public static final int INTAKE_HIGH = 7;
            public static final int INTAKE_LOW = 6;


        /* Example:
        public static final int LEFT_PINCER_OPEN = 5;
        */

            //PCM ports are not in the robot yet, using 0 and 1.
        }

        /**
         * There should be an entry here for each PDP breaker, preferrably in numerical order.
         * Note that only on device can be connected to each breaker, so the numbers should be unique.
         */
        public static class PDP {
        /* Example:
        public static final int ARM_VICTORSP = 0;
        */
        }

        /**
         * There should be an entry here for each Analgo port, preferrably in numerical order.
         * Note that for Analog only one device can connect to each port, so the numbers should be unique.
         */
        public static class Analog {
            public static final int MODE_SWITCH = 0;
        /*
        public static final int ARM_POTENTIOMETER = 7;
         */
        }

        /**
         * There should be an entry here for each DIO port, preferrably in numerical order.
         * Note that for DIO only one device can connect to each port, so the numbers should be unique.
         */
        public static class DIO {
            public static final int DRIVE_LEFT_B = 3;
            public static final int DRIVE_LEFT_A = 2;
            public static final int DRIVE_RIGHT_B = 0;
            public static final int DRIVE_RIGHT_A = 1;
        /* Example:
        public static final int ARM_FRONT_LIMIT = 0;
        */
        }


 }

