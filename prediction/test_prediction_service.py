import pytest
from unittest.mock import Mock, AsyncMock, patch
from prediction_service import PredictionService, PlayerStats

# 1. Mock the Neural Network Model
class MockModel:
    def __call__(self, x):
        import torch
        # Return dummy prediction tensor: [points, assists, rebounds]
        return torch.tensor([[0.8, 0.5, 0.4]])

@pytest.mark.asyncio
async def test_generate_prediction_success():
    # --- ARRANGE ---
    mock_model = MockModel()
    service = PredictionService(model=mock_model)

    # Mock Database Session
    mock_db = Mock()

    # Mock Stats Client response (The data coming from Java service)
    mock_stats_data = {
        "ppg": 25.0,
        "apg": 7.0,
        "rpg": 5.0,
        "gamesPlayed": 60
    }

    # Patch external dependencies
    with patch('prediction_service.stats_client.get_player_stats', new_callable=AsyncMock) as mock_get_stats, \
            patch('prediction_service.PredictionCache') as mock_cache, \
            patch('prediction_service.DataCollector') as mock_collector:

        # Setup mocks
        mock_cache.get.return_value = None  # Cache miss
        mock_get_stats.return_value = mock_stats_data # Successful API call

        # --- ACT ---
        result = await service.generate_prediction_for_player("LeBron", mock_db)

        # --- ASSERT ---
        assert result is not None
        assert result["player_name"] == "LeBron"
        assert "predicted_stats" in result
        assert result["predicted_stats"]["pts"] > 0

        # Verify stats were fetched
        mock_get_stats.assert_called_once_with("LeBron")
        # Verify result was saved to DB
        mock_db.add.assert_called_once()
        mock_db.commit.assert_called_once()

@pytest.mark.asyncio
async def test_handle_missing_player():
    service = PredictionService(model=MockModel())
    mock_db = Mock()

    # Simulate Stats Service returning None (Player not found)
    with patch('prediction_service.stats_client.get_player_stats', new_callable=AsyncMock) as mock_get_stats:
        mock_get_stats.return_value = None

        result = await service.generate_prediction_for_player("Unknown Player", mock_db)

        assert result is None