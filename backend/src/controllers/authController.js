const authService = require('../services/authService');

/**
 * Auth Controller.
 * Handles Signup, Login, Logout, and Current User Profile.
 */
exports.signup = async (req, res, next) => {
  try {
    const { fullName, email, phone, password } = req.body;
    const result = await authService.signup({ fullName, email, phone, password });

    res.status(201).json({
      success: true,
      message: 'Driver registration successful',
      data: result,
    });
  } catch (error) {
    next(error);
  }
};

exports.login = async (req, res, next) => {
  try {
    const { email, password } = req.body;
    const result = await authService.login({ email, password });

    res.status(200).json({
      success: true,
      message: 'Login successful',
      data: result,
    });
  } catch (error) {
    next(error);
  }
};

exports.me = async (req, res, next) => {
  try {
    const user = await authService.getMe(req.user.id);

    res.status(200).json({
      success: true,
      data: { user },
    });
  } catch (error) {
    next(error);
  }
};

exports.logout = (req, res) => {
  // Stateless JWT: acknowledged for Android client to clear token storage
  res.status(200).json({
    success: true,
    message: 'Logged out successfully. Client token cleared.',
  });
};
